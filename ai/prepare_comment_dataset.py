from __future__ import annotations

import json
import logging
import re
import unicodedata
from pathlib import Path
from typing import Iterable

import matplotlib.pyplot as plt
import pandas as pd


SUBJECT_FILE_MAP = {
    "DD": "Đạo đức",
    "KH": "Khoa học",
    "KHOC": "Khoa học",
    "LSDL": "Lịch sử và Địa lý",
    "T": "Toán",
    "TNXH": "Tự nhiên xã hội",
    "TV": "Tiếng Việt",
}

STANDARD_COLUMNS = [
    "class_name",
    "grade",
    "subject",
    "stage",
    "week",
    "lesson",
    "level",
    "content",
    "comment",
]

DEFAULT_COLUMNS_NO_HEADER = [
    "class_name",
    "grade",
    "subject",
    "stage",
    "week",
    "lesson",
    "content",
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
    "HTT": "Hoàn thành tốt",
    "HT": "Hoàn thành",
    "CHT": "Chưa hoàn thành",
}

INSTRUCTION_TEXT = (
    "Sinh nhận xét học bạ cho học sinh tiểu học dựa trên khối lớp, môn học, "
    "giai đoạn đánh giá, mức độ học tập và nội dung kiến thức."
)

CANONICAL_ALIASES = {
    "class_name": {
        "class_name",
        "class",
        "lop",
        "lớp",
        "ten_lop",
        "tên_lớp",
    },
    "grade": {
        "grade",
        "khoi",
        "khối",
        "khoi_lop",
        "khối_lớp",
        "grade_level",
    },
    "subject": {
        "subject",
        "mon",
        "môn",
        "mon_hoc",
        "môn_học",
        "ten_mon",
        "tên_môn",
        "subject_name",
    },
    "stage": {
        "stage",
        "giai_doan",
        "giai_đoạn",
        "dot",
        "đợt",
        "hoc_ky",
        "học_kỳ",
        "ky_danh_gia",
        "kỳ_đánh_giá",
        "term",
    },
    "week": {
        "week",
        "tuan",
        "tuần",
        "week_no",
    },
    "lesson": {
        "lesson",
        "tiet",
        "tiết",
        "period",
        "lesson_no",
    },
    "level": {
        "level",
        "muc_do",
        "mức_độ",
        "xep_loai",
        "xếp_loại",
        "evaluation",
    },
    "content": {
        "content",
        "noi_dung",
        "nội_dung",
        "kien_thuc",
        "kiến_thức",
        "yeu_cau_can_dat",
        "yêu_cầu_cần_đạt",
        "learning_objective",
        "lesson_title",
    },
    "comment": {
        "comment",
        "nhan_xet",
        "nhận_xét",
        "remark",
        "output",
        "comment_text",
    },
}

COLUMN_PRIORITY = {
    "content": [
        "learning_objective",
        "content",
        "noi_dung",
        "nội_dung",
        "kien_thuc",
        "kiến_thức",
        "yeu_cau_can_dat",
        "yêu_cầu_cần_đạt",
        "lesson_title",
    ],
}


def setup_logging() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")


def normalize_text(value: object) -> str | None:
    if pd.isna(value):
        return None
    text = str(value).strip()
    if not text:
        return None
    text = unicodedata.normalize("NFC", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def slugify_header(text: object) -> str:
    normalized = normalize_text(text)
    if not normalized:
        return ""
    lowered = normalized.lower()
    lowered = re.sub(r"[^\w\s]", " ", lowered, flags=re.UNICODE)
    lowered = re.sub(r"\s+", "_", lowered).strip("_")
    return lowered


def detect_subject_from_filename(file_path: Path) -> str | None:
    return SUBJECT_FILE_MAP.get(file_path.stem.upper())


def detect_subject_from_name(name: str) -> str | None:
    return SUBJECT_FILE_MAP.get(normalize_text(name).upper()) if normalize_text(name) else None


def discover_excel_files(base_dir: Path) -> list[Path]:
    raw_dir = base_dir / "data" / "raw"
    files = sorted(
        [
            path
            for path in raw_dir.glob("*.xlsx")
            if not path.name.startswith("~$")
        ]
    )
    if files:
        logging.info("Tìm thấy %s file Excel trong %s", len(files), raw_dir)
        return files

    fallback_files = sorted(
        [
            path
            for path in base_dir.glob("*.xlsx")
            if not path.name.startswith("~$")
        ]
    )
    if fallback_files:
        logging.warning(
            "Không tìm thấy file trong %s. Sử dụng tạm %s file Excel ở thư mục gốc project.",
            raw_dir,
            len(fallback_files),
        )
    return fallback_files


def list_excel_sheets(file_path: Path) -> list[str]:
    xls = pd.ExcelFile(file_path, engine="openpyxl")
    return xls.sheet_names


def has_header_row(df_preview: pd.DataFrame) -> bool:
    if df_preview.empty:
        return False
    first_row = df_preview.iloc[0].tolist()
    normalized = {slugify_header(value) for value in first_row if slugify_header(value)}
    if not normalized:
        return False
    all_aliases = set().union(*CANONICAL_ALIASES.values())
    return len(normalized & all_aliases) >= 2


def read_excel_flexible(file_path: Path, sheet_name: str | int | None = 0) -> pd.DataFrame:
    preview = pd.read_excel(file_path, sheet_name=sheet_name, header=None, nrows=3, engine="openpyxl")
    if preview.empty:
        return pd.DataFrame()

    if has_header_row(preview):
        df = pd.read_excel(file_path, sheet_name=sheet_name, engine="openpyxl")
    else:
        df = pd.read_excel(file_path, sheet_name=sheet_name, header=None, engine="openpyxl")
        column_count = df.shape[1]
        default_columns = DEFAULT_COLUMNS_NO_HEADER[:]
        if column_count > len(default_columns):
            extra_columns = [f"extra_{idx}" for idx in range(1, column_count - len(default_columns) + 1)]
            default_columns.extend(extra_columns)
        df.columns = default_columns[:column_count]
    df = df.dropna(how="all").copy()
    df = df.dropna(axis=1, how="all").copy()
    return df


def standardize_columns(df: pd.DataFrame) -> pd.DataFrame:
    column_groups: dict[str, list[str]] = {column: [] for column in STANDARD_COLUMNS}
    slug_to_columns: dict[str, list[str]] = {}

    for column in df.columns:
        slug = slugify_header(column)
        slug_to_columns.setdefault(slug, []).append(column)
        mapped = False
        for canonical_name, aliases in CANONICAL_ALIASES.items():
            if slug in aliases:
                column_groups[canonical_name].append(column)
                mapped = True
                break
        if not mapped and column in STANDARD_COLUMNS:
            column_groups[column].append(column)

    standardized = pd.DataFrame(index=df.index)
    for canonical_name in STANDARD_COLUMNS:
        source_columns = column_groups[canonical_name][:]
        priority_slugs = COLUMN_PRIORITY.get(canonical_name, [])
        if priority_slugs:
            prioritized_columns: list[str] = []
            for slug in priority_slugs:
                prioritized_columns.extend(slug_to_columns.get(slug, []))
            prioritized_columns.extend([col for col in source_columns if col not in prioritized_columns])
            source_columns = prioritized_columns

        if not source_columns:
            standardized[canonical_name] = pd.NA
            continue

        combined = df[source_columns[0]]
        for extra_column in source_columns[1:]:
            combined = combined.combine_first(df[extra_column])
        standardized[canonical_name] = combined

    return standardized


def fill_subject_column(df: pd.DataFrame, file_path: Path) -> pd.DataFrame:
    detected_subject = detect_subject_from_filename(file_path)
    if detected_subject is None and df["subject"].isna().all():
        raise ValueError(
            f"Không xác định được môn học từ tên file {file_path.name}. "
            "Hãy dùng tên file đúng quy ước hoặc thêm cột subject."
        )

    df["subject"] = df["subject"].apply(normalize_text)
    if detected_subject is not None:
        df["subject"] = df["subject"].fillna(detected_subject)
        df["subject"] = detected_subject
    return df


def fill_subject_column_with_fallback(
    df: pd.DataFrame,
    file_path: Path,
    sheet_name: str | None = None,
) -> pd.DataFrame:
    detected_subject = None
    if sheet_name:
        detected_subject = detect_subject_from_name(sheet_name)
    if detected_subject is None:
        detected_subject = detect_subject_from_filename(file_path)

    if detected_subject is None and df["subject"].isna().all():
        raise ValueError(
            f"Không xác định được môn học từ file {file_path.name}"
            + (f", sheet {sheet_name}" if sheet_name else "")
            + ". Hãy dùng tên file/sheet đúng quy ước hoặc thêm cột subject."
        )

    df["subject"] = df["subject"].apply(normalize_text)
    if detected_subject is not None:
        df["subject"] = detected_subject
    return df


def extract_grade_value(value: object) -> str | None:
    text = normalize_text(value)
    if not text:
        return None

    match = re.search(r"[1-5]", text)
    if match:
        return match.group(0)

    return None


def extract_grade_from_class_name(class_name: object) -> str | None:
    return extract_grade_value(class_name)


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
    compact = re.sub(r"\s+", "", text.upper())
    compact_no_diacritics = compact
    if compact_no_diacritics == "T":
        return "Hoàn thành tốt"
    if compact_no_diacritics == "H":
        return "Hoàn thành"
    if compact_no_diacritics == "C":
        return "Chưa hoàn thành"
    return LEVEL_MAP.get(compact, text)


def normalize_scalar_columns(df: pd.DataFrame) -> pd.DataFrame:
    for column in STANDARD_COLUMNS:
        df[column] = df[column].apply(normalize_text)

    df["grade"] = df["grade"].apply(extract_grade_value)
    missing_grade_mask = df["grade"].isna() & df["class_name"].notna()
    df.loc[missing_grade_mask, "grade"] = df.loc[missing_grade_mask, "class_name"].apply(extract_grade_from_class_name)
    df["stage"] = df["stage"].apply(normalize_stage)
    df["level"] = df["level"].apply(normalize_level)
    return df


def validate_required_columns(df: pd.DataFrame, file_path: Path) -> None:
    missing = [column for column in REQUIRED_COLUMNS if column not in df.columns or df[column].isna().all()]
    if missing:
        raise ValueError(f"File {file_path.name} thiếu cột bắt buộc: {', '.join(missing)}")


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
        ("Khối", row.get("grade")),
        ("Môn", row.get("subject")),
        ("Giai đoạn", row.get("stage")),
        ("Tuần", row.get("week")),
        ("Tiết", row.get("lesson")),
        ("Mức độ", row.get("level")),
        ("Nội dung", row.get("content")),
    ]
    lines = [f"{label}: {value}" for label, value in mapping if normalize_text(value)]
    return "\n".join(lines)


def add_model_columns(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["instruction"] = INSTRUCTION_TEXT
    df["model_input"] = df.apply(build_model_input, axis=1)
    df["model_output"] = df["comment"]
    return df


def create_subject_stats(df: pd.DataFrame) -> pd.DataFrame:
    stats = (
        df.groupby("subject", dropna=False)
        .size()
        .reset_index(name="count")
        .sort_values(["count", "subject"], ascending=[False, True])
        .reset_index(drop=True)
    )
    return stats


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
        valid_parts.append(shuffled.iloc[train_count:train_count + valid_count])
        test_parts.append(shuffled.iloc[train_count + valid_count:train_count + valid_count + test_count])

    train_df = pd.concat(train_parts, ignore_index=True) if train_parts else pd.DataFrame(columns=df.columns)
    valid_df = pd.concat(valid_parts, ignore_index=True) if valid_parts else pd.DataFrame(columns=df.columns)
    test_df = pd.concat(test_parts, ignore_index=True) if test_parts else pd.DataFrame(columns=df.columns)

    train_df = train_df.sample(frac=1, random_state=seed).reset_index(drop=True)
    valid_df = valid_df.sample(frac=1, random_state=seed).reset_index(drop=True)
    test_df = test_df.sample(frac=1, random_state=seed).reset_index(drop=True)
    return train_df, valid_df, test_df


def to_jsonl_records(df: pd.DataFrame) -> list[dict[str, str]]:
    records: list[dict[str, str]] = []
    for _, row in df.iterrows():
        records.append(
            {
                "instruction": row["instruction"],
                "input": row["model_input"],
                "output": row["model_output"],
            }
        )
    return records


def write_jsonl(records: Iterable[dict[str, str]], output_path: Path) -> None:
    with output_path.open("w", encoding="utf-8") as file:
        for record in records:
            file.write(json.dumps(record, ensure_ascii=False) + "\n")


def export_subject_chart(stats_df: pd.DataFrame, output_path: Path) -> None:
    if stats_df.empty:
        return

    plt.figure(figsize=(10, 6))
    bars = plt.bar(stats_df["subject"], stats_df["count"], color="#2a6f97")
    plt.title("So luong du lieu theo mon hoc")
    plt.xlabel("Mon hoc")
    plt.ylabel("So mau")
    plt.xticks(rotation=20, ha="right")

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

    df.to_excel(output_dir / "dataset_clean.xlsx", index=False)
    stats_df.to_excel(output_dir / "stats_by_subject.xlsx", index=False)
    export_subject_chart(stats_df, output_dir / "stats_by_subject.png")
    train_df.to_excel(output_dir / "train.xlsx", index=False)
    valid_df.to_excel(output_dir / "valid.xlsx", index=False)
    test_df.to_excel(output_dir / "test.xlsx", index=False)

    write_jsonl(to_jsonl_records(train_df), output_dir / "train.jsonl")
    write_jsonl(to_jsonl_records(valid_df), output_dir / "valid.jsonl")
    write_jsonl(to_jsonl_records(test_df), output_dir / "test.jsonl")
    write_jsonl(to_jsonl_records(df), output_dir / "all.jsonl")


def load_and_standardize_sheet(file_path: Path, sheet_name: str | int | None = 0) -> pd.DataFrame:
    sheet_label = f"{file_path.name} | sheet={sheet_name}" if sheet_name is not None else file_path.name
    logging.info("Đang đọc file: %s", sheet_label)
    raw_df = read_excel_flexible(file_path, sheet_name=sheet_name)
    if raw_df.empty:
        logging.warning("Bỏ qua sheet rỗng: %s", sheet_label)
        return pd.DataFrame(columns=STANDARD_COLUMNS)

    standardized_df = standardize_columns(raw_df)
    validate_required_columns(standardized_df, file_path)
    standardized_df = fill_subject_column_with_fallback(
        standardized_df,
        file_path,
        str(sheet_name) if sheet_name is not None else None,
    )
    standardized_df = normalize_scalar_columns(standardized_df)
    return standardized_df


def load_and_standardize_file(file_path: Path) -> pd.DataFrame:
    sheet_names = list_excel_sheets(file_path)
    if len(sheet_names) <= 1:
        return load_and_standardize_sheet(file_path, sheet_name=sheet_names[0] if sheet_names else 0)

    sheet_frames: list[pd.DataFrame] = []
    for sheet_name in sheet_names:
        try:
            sheet_df = load_and_standardize_sheet(file_path, sheet_name=sheet_name)
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
    base_dir = Path(__file__).resolve().parent
    output_dir = base_dir / "data" / "processed"

    excel_files = discover_excel_files(base_dir)
    if not excel_files:
        raise FileNotFoundError(
            "Không tìm thấy file Excel nào. Hãy đặt dữ liệu vào thư mục data/raw/."
        )

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


if __name__ == "__main__":
    main()
