from __future__ import annotations

import json
import logging
import re
import unicodedata
from pathlib import Path
from typing import Iterable

import matplotlib.font_manager as font_manager
import matplotlib.pyplot as plt
import pandas as pd


RAW_DATA_DIR = Path("data/raw")
OUTPUT_DATA_DIR = Path("data/processed")

SUBJECT_NAME_MAP = {
    # Đạo đức
    "DD": "Đạo đức",
    "ĐĐ": "Đạo đức",
    "DAO DUC": "Đạo đức",
    "DAODUC": "Đạo đức",

    # Khoa học
    "KH": "Khoa học",
    "KHOA HOC": "Khoa học",
    "KHOAHOC": "Khoa học",

    # Tiếng Việt
    "TV": "Tiếng Việt",
    "TIENG VIET": "Tiếng Việt",
    "TIENGVIET": "Tiếng Việt",

    # Toán
    "T": "Toán",
    "TOAN": "Toán",

    # Tự nhiên và Xã hội
    "TNXH": "Tự nhiên và Xã hội",
    "TU NHIEN VA XA HOI": "Tự nhiên và Xã hội",
    "TUNHIENVAXAHOI": "Tự nhiên và Xã hội",

    # Lịch sử và Địa lí
    "LSDL": "Lịch sử và Địa lí",
    "LSĐL": "Lịch sử và Địa lí",
    "LICH SU VA DIA LI": "Lịch sử và Địa lí",
    "LICHSUVADIALI": "Lịch sử và Địa lí",

    # Công nghệ
    "CN": "Công nghệ",
    "CONG NGHE": "Công nghệ",
    "CONGNGHE": "Công nghệ",

    # Hoạt động trải nghiệm
    "HDTN": "Hoạt động trải nghiệm",
    "HOAT DONG TRAI NGHIEM": "Hoạt động trải nghiệm",
    "HOATDONGTRAINGHIEM": "Hoạt động trải nghiệm",

    # Mĩ thuật
    "MT": "Mĩ thuật",
    "MI THUAT": "Mĩ thuật",
    "MITHUAT": "Mĩ thuật",
    "MY THUAT": "Mĩ thuật",
    "MYTHUAT": "Mĩ thuật",

    # Giáo dục thể chất
    "GDTC": "Giáo dục thể chất",
    "GIAO DUC THE CHAT": "Giáo dục thể chất",
    "GIAODUCTHECHAT": "Giáo dục thể chất",

    # Âm nhạc
    "AN": "Âm nhạc",
    "AM NHAC": "Âm nhạc",
    "AMNHAC": "Âm nhạc",

    # Tin học
    "TH": "Tin học",
    "TIN HOC": "Tin học",
    "TINHOC": "Tin học",

    # Tiếng Anh
    "TA": "Tiếng Anh",
    "TIENG ANH": "Tiếng Anh",
    "TIENGANH": "Tiếng Anh",
}

EXPECTED_SUBJECTS = [
    "Tiếng Việt",
    "Toán",
    "Đạo đức",
    "Khoa học",
    "Tự nhiên và Xã hội",
    "Lịch sử và Địa lí",
    "Công nghệ",
    "Hoạt động trải nghiệm",
    "Mĩ thuật",
    "Giáo dục thể chất",
    "Âm nhạc",
    "Tin học",
    "Tiếng Anh",
]

STANDARD_COLUMNS = [
    "student_id",
    "student_name",
    "class_name",
    "grade",
    "subject",
    "stage",
    "week",
    "lesson",
    "lesson_title",
    "content",
    "level",
    "attendance_absent",
    "attendance_full",
    "participation_level",
    "behavior_tag",
    "textbook_series",
    "comment",
]

TONG_HOP_COLUMNS = [
    "student_id",
    "student_name",
    "class_name",
    "grade",
    "subject",
    "stage",
    "week",
    "lesson",
    "lesson_title",
    "content",
    "level",
    "attendance_absent",
    "attendance_full",
    "participation_level",
    "behavior_tag",
    "textbook_series",
    "comment",
]

REQUIRED_COLUMNS = ["content", "comment"]

STAGE_MAP = {
    "GK1": "Giữa học kỳ 1",
    "CK1": "Cuối học kỳ 1",
    "GK2": "Giữa học kỳ 2",
    "CK2": "Cuối học kỳ 2",
}

LEVEL_MAP = {
    "T": "Hoàn thành tốt",
    "HTT": "Hoàn thành tốt",
    "H": "Hoàn thành",
    "HT": "Hoàn thành",
    "C": "Chưa hoàn thành",
    "CHT": "Chưa hoàn thành",
}

INSTRUCTION_TEXT = (
    "Sinh nhận xét học bạ cho học sinh tiểu học dựa trên khối lớp, môn học, "
    "giai đoạn đánh giá, tuần, tiết, mức độ học tập và nội dung kiến thức."
)

CANONICAL_ALIASES = {
    "student_id": {"student_id", "ma_hoc_sinh", "mã_học_sinh", "ma_hs", "id"},
    "student_name": {"student_name", "ten_hoc_sinh", "tên_học_sinh", "ho_ten", "họ_tên"},
    "class_name": {"class_name", "class", "lop", "lớp", "ten_lop", "tên_lớp"},
    "grade": {"grade", "grade_level", "khoi", "khối", "khoi_lop", "khối_lớp"},
    "subject": {"subject", "subject_name", "mon", "môn", "mon_hoc", "môn_học", "ten_mon", "tên_môn"},
    "stage": {"stage", "term", "giai_doan", "giai_đoạn", "dot", "đợt", "hoc_ky", "học_kỳ"},
    "week": {"week", "week_no", "tuan", "tuần"},
    "lesson": {"lesson", "lesson_no", "tiet", "tiết", "period"},
    "lesson_title": {"lesson_title", "ten_bai", "tên_bài", "bai_hoc", "bài_học"},
    "content": {
        "content",
        "learning_objective",
        "noi_dung",
        "nội_dung",
        "kien_thuc",
        "kiến_thức",
        "yeu_cau_can_dat",
        "yêu_cầu_cần_đạt",
    },
    "level": {"level", "evaluation", "muc_do", "mức_độ", "xep_loai", "xếp_loại"},
    "attendance_absent": {"attendance_absent", "vang", "vắng", "so_buoi_vang", "số_buổi_vắng"},
    "attendance_full": {"attendance_full", "du", "đủ", "di_hoc_day_du", "đi_học_đầy_đủ"},
    "participation_level": {"participation_level", "tham_gia", "muc_do_tham_gia", "mức_độ_tham_gia"},
    "behavior_tag": {"behavior_tag", "hanh_vi", "hành_vi", "pham_chat", "phẩm_chất"},
    "textbook_series": {"textbook_series", "bo_sach", "bộ_sách", "sach", "sách"},
    "comment": {"comment", "comment_text", "nhan_xet", "nhận_xét", "remark", "output"},
}

CONTENT_PRIORITY = ["learning_objective", "content", "noi_dung", "nội_dung", "lesson_title"]


def setup_logging() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")


def setup_matplotlib_font() -> None:
    preferred_fonts = ["Segoe UI", "Arial", "Tahoma", "DejaVu Sans"]
    available_fonts = {font.name for font in font_manager.fontManager.ttflist}
    for font_name in preferred_fonts:
        if font_name in available_fonts:
            plt.rcParams["font.family"] = font_name
            break
    plt.rcParams["axes.unicode_minus"] = False


def normalize_text(value: object) -> str | None:
    if pd.isna(value):
        return None
    text = unicodedata.normalize("NFC", str(value)).strip()
    text = re.sub(r"\s+", " ", text)
    return text or None


def strip_diacritics(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    return "".join(char for char in normalized if unicodedata.category(char) != "Mn")


def slugify_header(value: object) -> str:
    text = normalize_text(value)
    if not text:
        return ""
    text = text.lower()
    text = re.sub(r"[^\w\s]", " ", text, flags=re.UNICODE)
    return re.sub(r"\s+", "_", text).strip("_")


def subject_key(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None
    plain = strip_diacritics(text).upper()
    plain = re.sub(r"[^A-Z0-9]+", " ", plain).strip()
    compact = plain.replace(" ", "")
    return compact if compact in SUBJECT_NAME_MAP else plain


def canonical_subject(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None
    key = subject_key(text)
    return SUBJECT_NAME_MAP.get(key or "", text)


def discover_excel_files(base_dir: Path) -> list[Path]:
    raw_dir = base_dir / RAW_DATA_DIR
    files = sorted(path for path in raw_dir.glob("*.xlsx") if not path.name.startswith("~$"))
    if files:
        logging.info("Tìm thấy %s file Excel trong %s", len(files), raw_dir)
        return files

    fallback_files = sorted(path for path in base_dir.glob("*.xlsx") if not path.name.startswith("~$"))
    if fallback_files:
        logging.warning("Không thấy file trong %s, dùng %s file Excel ở thư mục gốc.", raw_dir, len(fallback_files))
    return fallback_files


def list_excel_sheets(file_path: Path) -> list[str]:
    return pd.ExcelFile(file_path, engine="openpyxl").sheet_names


def has_header_row(preview_df: pd.DataFrame) -> bool:
    if preview_df.empty:
        return False
    first_row_slugs = {slugify_header(value) for value in preview_df.iloc[0].tolist()}
    first_row_slugs.discard("")
    aliases = set().union(*CANONICAL_ALIASES.values())
    return len(first_row_slugs & aliases) >= 2


def read_excel_flexible(file_path: Path, sheet_name: str | int) -> pd.DataFrame:
    preview_df = pd.read_excel(file_path, sheet_name=sheet_name, header=None, nrows=3, engine="openpyxl")
    if preview_df.empty:
        return pd.DataFrame()

    if has_header_row(preview_df):
        df = pd.read_excel(file_path, sheet_name=sheet_name, engine="openpyxl")
    else:
        df = pd.read_excel(file_path, sheet_name=sheet_name, header=None, engine="openpyxl")
        columns = TONG_HOP_COLUMNS[: df.shape[1]]
        if df.shape[1] > len(columns):
            columns.extend(f"extra_{idx}" for idx in range(1, df.shape[1] - len(columns) + 1))
        df.columns = columns

    df = df.dropna(how="all").dropna(axis=1, how="all").copy()
    return df


def standardize_columns(df: pd.DataFrame) -> pd.DataFrame:
    source_by_slug: dict[str, list[object]] = {}
    grouped_sources: dict[str, list[object]] = {column: [] for column in STANDARD_COLUMNS}

    for column in df.columns:
        slug = slugify_header(column)
        source_by_slug.setdefault(slug, []).append(column)
        for canonical_name, aliases in CANONICAL_ALIASES.items():
            if slug in aliases:
                grouped_sources[canonical_name].append(column)
                break

    standardized = pd.DataFrame(index=df.index)
    for canonical_name in STANDARD_COLUMNS:
        source_columns = grouped_sources[canonical_name][:]
        if canonical_name == "content":
            prioritized: list[object] = []
            for slug in CONTENT_PRIORITY:
                prioritized.extend(source_by_slug.get(slug, []))
            source_columns = prioritized + [column for column in source_columns if column not in prioritized]

        if not source_columns:
            standardized[canonical_name] = pd.NA
            continue

        combined = df[source_columns[0]]
        for extra_column in source_columns[1:]:
            combined = combined.combine_first(df[extra_column])
        standardized[canonical_name] = combined

    return standardized


def fill_subject(df: pd.DataFrame, file_path: Path, sheet_name: str) -> pd.DataFrame:
    df = df.copy()
    df["subject"] = df["subject"].apply(canonical_subject)
    fallback_subject = canonical_subject(sheet_name) or canonical_subject(file_path.stem)

    if df["subject"].isna().all() and fallback_subject is None:
        raise ValueError(f"Không xác định được môn học từ file {file_path.name}, sheet {sheet_name}.")

    if fallback_subject:
        df["subject"] = df["subject"].fillna(fallback_subject)
    return df


def extract_grade(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None
    match = re.search(r"[1-5]", text)
    return match.group(0) if match else None


def normalize_stage(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None
    compact = re.sub(r"\s+", "", text.upper())
    return STAGE_MAP.get(compact, text)


def normalize_level(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None
    compact = re.sub(r"\s+", "", strip_diacritics(text).upper())
    return LEVEL_MAP.get(compact, text)


def normalize_scalar_columns(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    for column in STANDARD_COLUMNS:
        df[column] = df[column].apply(normalize_text)

    df["subject"] = df["subject"].apply(canonical_subject)
    df["grade"] = df["grade"].apply(extract_grade)
    missing_grade = df["grade"].isna() & df["class_name"].notna()
    df.loc[missing_grade, "grade"] = df.loc[missing_grade, "class_name"].apply(extract_grade)
    df["stage"] = df["stage"].apply(normalize_stage)
    df["level"] = df["level"].apply(normalize_level)
    return df


def validate_required_columns(df: pd.DataFrame, file_path: Path, sheet_name: str) -> None:
    missing = [column for column in REQUIRED_COLUMNS if column not in df.columns or df[column].isna().all()]
    if missing:
        raise ValueError(f"{file_path.name} / {sheet_name} thiếu cột bắt buộc: {', '.join(missing)}")


def clean_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    before_count = len(df)
    df = df.dropna(how="all").copy()
    df = df[df["content"].notna() & df["comment"].notna()].copy()
    df = df[df["comment"].str.len() >= 10].copy()
    df = df.drop_duplicates().reset_index(drop=True)
    logging.info("Số dòng trước làm sạch: %s", before_count)
    logging.info("Số dòng sau làm sạch: %s", len(df))
    return df


def build_model_input(row: pd.Series) -> str:
    mapping = [
        ("Học sinh", row.get("student_name")),
        ("Lớp", row.get("class_name")),
        ("Khối", row.get("grade")),
        ("Môn", row.get("subject")),
        ("Giai đoạn", row.get("stage")),
        ("Tuần", row.get("week")),
        ("Tiết", row.get("lesson")),
        ("Bài học", row.get("lesson_title")),
        ("Yêu cầu cần đạt", row.get("content")),
        ("Mức độ", row.get("level")),
        ("Mức tham gia", row.get("participation_level")),
        ("Phẩm chất/Hành vi", row.get("behavior_tag")),
        ("Bộ sách", row.get("textbook_series")),
    ]
    return "\n".join(f"{label}: {value}" for label, value in mapping if normalize_text(value))


def add_model_columns(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["instruction"] = INSTRUCTION_TEXT
    df["model_input"] = df.apply(build_model_input, axis=1)
    df["model_output"] = df["comment"]
    return df


def create_subject_stats(df: pd.DataFrame) -> pd.DataFrame:
    actual_stats = df.groupby("subject", dropna=False).size().reset_index(name="count")
    expected_stats = pd.DataFrame({"subject": EXPECTED_SUBJECTS})
    stats = expected_stats.merge(actual_stats, on="subject", how="left")
    stats["count"] = stats["count"].fillna(0).astype(int)

    extra_stats = actual_stats[~actual_stats["subject"].isin(EXPECTED_SUBJECTS)].copy()
    if not extra_stats.empty:
        stats = pd.concat([stats, extra_stats], ignore_index=True)

    return stats.reset_index(drop=True)


def get_missing_expected_subjects(stats_df: pd.DataFrame) -> list[str]:
    missing_df = stats_df[stats_df["subject"].isin(EXPECTED_SUBJECTS) & (stats_df["count"] == 0)]
    return missing_df["subject"].tolist()


def split_group_indices(group_size: int) -> tuple[int, int, int]:
    if group_size == 1:
        return 1, 0, 0
    if group_size == 2:
        return 1, 1, 0
    if group_size == 3:
        return 1, 1, 1

    train_count = max(1, int(round(group_size * 0.8)))
    valid_count = max(1, int(round(group_size * 0.1)))
    test_count = max(1, group_size - train_count - valid_count)

    while train_count + valid_count + test_count > group_size:
        if train_count >= valid_count and train_count >= test_count and train_count > 1:
            train_count -= 1
        elif valid_count >= test_count and valid_count > 1:
            valid_count -= 1
        else:
            test_count -= 1

    while train_count + valid_count + test_count < group_size:
        train_count += 1

    return train_count, valid_count, test_count


def stratified_split_by_subject(df: pd.DataFrame, seed: int = 42) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    train_parts: list[pd.DataFrame] = []
    valid_parts: list[pd.DataFrame] = []
    test_parts: list[pd.DataFrame] = []

    for _, group in df.groupby("subject", dropna=False):
        shuffled = group.sample(frac=1, random_state=seed).reset_index(drop=True)
        train_count, valid_count, test_count = split_group_indices(len(shuffled))

        train_parts.append(shuffled.iloc[:train_count])
        valid_parts.append(shuffled.iloc[train_count : train_count + valid_count])
        test_parts.append(shuffled.iloc[train_count + valid_count : train_count + valid_count + test_count])

    train_df = pd.concat(train_parts, ignore_index=True) if train_parts else pd.DataFrame(columns=df.columns)
    valid_df = pd.concat(valid_parts, ignore_index=True) if valid_parts else pd.DataFrame(columns=df.columns)
    test_df = pd.concat(test_parts, ignore_index=True) if test_parts else pd.DataFrame(columns=df.columns)

    return (
        train_df.sample(frac=1, random_state=seed).reset_index(drop=True),
        valid_df.sample(frac=1, random_state=seed).reset_index(drop=True),
        test_df.sample(frac=1, random_state=seed).reset_index(drop=True),
    )


def to_jsonl_records(df: pd.DataFrame) -> list[dict[str, str]]:
    return [
        {
            "instruction": row["instruction"],
            "input": row["model_input"],
            "output": row["model_output"],
        }
        for _, row in df.iterrows()
    ]


def write_jsonl(records: Iterable[dict[str, str]], output_path: Path) -> None:
    with output_path.open("w", encoding="utf-8") as file:
        for record in records:
            file.write(json.dumps(record, ensure_ascii=False) + "\n")


def write_missing_subjects(missing_subjects: list[str], output_path: Path) -> None:
    with output_path.open("w", encoding="utf-8") as file:
        if not missing_subjects:
            file.write("Không thiếu môn nào trong danh sách môn dự kiến.\n")
            return

        file.write("Các môn chưa có dữ liệu trong file nguồn hiện tại:\n")
        for subject in missing_subjects:
            file.write(f"- {subject}\n")


def export_subject_chart(stats_df: pd.DataFrame, output_path: Path) -> None:
    if stats_df.empty:
        return

    plt.figure(figsize=(14, 7))
    colors = ["#2a6f97" if count > 0 else "#c9d3dc" for count in stats_df["count"]]
    bars = plt.bar(stats_df["subject"], stats_df["count"], color=colors)
    plt.title("Số lượng dữ liệu theo môn học")
    plt.xlabel("Môn học")
    plt.ylabel("Số mẫu")
    plt.xticks(rotation=30, ha="right")

    for bar, count in zip(bars, stats_df["count"]):
        plt.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height(),
            str(count),
            ha="center",
            va="bottom",
            fontsize=9,
        )

    plt.tight_layout()
    plt.savefig(output_path, dpi=200, bbox_inches="tight")
    plt.close()


def export_outputs(
    df: pd.DataFrame,
    stats_df: pd.DataFrame,
    train_df: pd.DataFrame,
    valid_df: pd.DataFrame,
    test_df: pd.DataFrame,
    output_dir: Path,
) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    missing_subjects = get_missing_expected_subjects(stats_df)

    df.to_excel(output_dir / "dataset_clean.xlsx", index=False)
    stats_df.to_excel(output_dir / "stats_by_subject.xlsx", index=False)
    write_missing_subjects(missing_subjects, output_dir / "missing_subjects.txt")
    export_subject_chart(stats_df, output_dir / "stats_by_subject.png")
    train_df.to_excel(output_dir / "train.xlsx", index=False)
    valid_df.to_excel(output_dir / "valid.xlsx", index=False)
    test_df.to_excel(output_dir / "test.xlsx", index=False)

    write_jsonl(to_jsonl_records(train_df), output_dir / "train.jsonl")
    write_jsonl(to_jsonl_records(valid_df), output_dir / "valid.jsonl")
    write_jsonl(to_jsonl_records(test_df), output_dir / "test.jsonl")
    write_jsonl(to_jsonl_records(df), output_dir / "all.jsonl")


def load_and_standardize_sheet(file_path: Path, sheet_name: str) -> pd.DataFrame:
    logging.info("Đang đọc: %s | sheet=%s", file_path.name, sheet_name)
    raw_df = read_excel_flexible(file_path, sheet_name=sheet_name)
    if raw_df.empty:
        logging.warning("Bỏ qua sheet rỗng: %s | %s", file_path.name, sheet_name)
        return pd.DataFrame(columns=STANDARD_COLUMNS)

    standardized_df = standardize_columns(raw_df)
    validate_required_columns(standardized_df, file_path, sheet_name)
    standardized_df = fill_subject(standardized_df, file_path, sheet_name)
    standardized_df = normalize_scalar_columns(standardized_df)
    standardized_df = standardized_df[
        standardized_df["content"].notna() | standardized_df["comment"].notna()
    ].reset_index(drop=True)
    return standardized_df


def load_and_standardize_file(file_path: Path) -> pd.DataFrame:
    sheet_frames: list[pd.DataFrame] = []
    for sheet_name in list_excel_sheets(file_path):
        try:
            sheet_df = load_and_standardize_sheet(file_path, sheet_name)
        except ValueError as exc:
            logging.warning("Bỏ qua sheet %s của %s: %s", sheet_name, file_path.name, exc)
            continue
        if not sheet_df.empty:
            sheet_frames.append(sheet_df)

    if not sheet_frames:
        return pd.DataFrame(columns=STANDARD_COLUMNS)
    return pd.concat(sheet_frames, ignore_index=True)


def main() -> None:
    setup_logging()
    setup_matplotlib_font()
    base_dir = Path(__file__).resolve().parent
    output_dir = base_dir / OUTPUT_DATA_DIR

    excel_files = discover_excel_files(base_dir)
    if not excel_files:
        raise FileNotFoundError("Không tìm thấy file Excel nào. Hãy đặt dữ liệu vào thư mục data/raw/.")

    dataframes = [load_and_standardize_file(file_path) for file_path in excel_files]
    combined_df = pd.concat(dataframes, ignore_index=True)
    if combined_df.empty:
        raise ValueError("Không có dữ liệu hợp lệ sau khi đọc các file Excel.")

    cleaned_df = clean_dataframe(combined_df)
    if cleaned_df.empty:
        raise ValueError("Dữ liệu rỗng sau khi làm sạch.")

    enriched_df = add_model_columns(cleaned_df)
    stats_df = create_subject_stats(enriched_df)
    train_df, valid_df, test_df = stratified_split_by_subject(enriched_df, seed=42)

    export_outputs(enriched_df, stats_df, train_df, valid_df, test_df, output_dir)

    logging.info("Đã xuất dữ liệu vào: %s", output_dir)
    logging.info("Train: %s | Valid: %s | Test: %s", len(train_df), len(valid_df), len(test_df))
    logging.info("Thống kê số lượng theo môn học:")
    for _, row in stats_df.iterrows():
        logging.info("- %s: %s", row["subject"], row["count"])

    missing_subjects = get_missing_expected_subjects(stats_df)
    if missing_subjects:
        logging.warning("Các môn chưa có dữ liệu trong file nguồn: %s", ", ".join(missing_subjects))


if __name__ == "__main__":
    main()
