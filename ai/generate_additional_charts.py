import os
import sys
import pandas as pd
import matplotlib.pyplot as plt

# Reconfigure stdout to use UTF-8 for Windows compatibility
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass  # For python versions that don't support reconfigure

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    dataset_path = os.path.join(base_dir, "data", "processed", "dataset_clean.xlsx")
    output_dir = os.path.join(base_dir, "outputs")
    os.makedirs(output_dir, exist_ok=True)
    
    if not os.path.isfile(dataset_path):
        print(f"Error: Could not find dataset_clean.xlsx at {dataset_path}")
        return

    # Load dataset
    print(f"Loading dataset from {dataset_path}...")
    df = pd.read_excel(dataset_path)

    # 1. Plot Evaluation Level Distribution (Pie Chart)
    if "level" in df.columns:
        level_counts = df["level"].value_counts(dropna=True)
        print("Evaluation level counts:\n", level_counts)
        
        plt.figure(figsize=(8, 8), dpi=200)
        colors = ["#2ecc71", "#3498db", "#e74c3c", "#f1c40f"][:len(level_counts)]
        
        plt.pie(
            level_counts, 
            labels=level_counts.index, 
            autopct="%1.1f%%", 
            startangle=140, 
            colors=colors,
            textprops={"fontsize": 12, "weight": "bold"}
        )
        plt.title("Phân bố mức độ đánh giá trong tập dữ liệu", fontsize=14, fontweight="bold", pad=20)
        
        pie_path = os.path.join(output_dir, "eval_level_distribution.png")
        plt.savefig(pie_path, bbox_inches="tight")
        plt.close()
        print(f"Saved evaluation level distribution chart to {pie_path}")
    else:
        print("Warning: 'level' column not found in dataset.")

    # 2. Plot Comment Word-Length Distribution (Histogram using matplotlib)
    if "comment" in df.columns:
        word_counts = df["comment"].dropna().apply(lambda x: len(str(x).split()))
        print(f"Comment word count statistics:\n{word_counts.describe()}")

        plt.figure(figsize=(10, 6), dpi=200)
        
        # Plot distribution using standard matplotlib plt.hist
        plt.hist(word_counts, bins=15, color="#9b59b6", edgecolor="white", alpha=0.7)
        
        plt.title("Biểu đồ phân bố số lượng từ trong câu nhận xét", fontsize=14, fontweight="bold", pad=15)
        plt.xlabel("Số lượng từ (words)", fontsize=11, labelpad=10)
        plt.ylabel("Số lượng câu nhận xét (tần suất)", fontsize=11, labelpad=10)
        plt.grid(True, linestyle="--", alpha=0.5)
        
        hist_path = os.path.join(output_dir, "comment_length_distribution.png")
        plt.savefig(hist_path, bbox_inches="tight")
        plt.close()
        print(f"Saved comment word-length distribution chart to {hist_path}")
    else:
        print("Warning: 'comment' column not found in dataset.")

if __name__ == "__main__":
    main()
