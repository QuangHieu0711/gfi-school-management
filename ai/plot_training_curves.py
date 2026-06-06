import os
import json
import matplotlib.pyplot as plt

def main():
    # Define paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    trainer_state_path = os.path.join(
        base_dir,
        "gfi_comments_lora_final",
        "saves",
        "qwen2_5_1_5b",
        "gfi_comments_lora",
        "checkpoint-2469",
        "trainer_state.json"
    )
    output_dir = os.path.join(base_dir, "outputs")
    os.makedirs(output_dir, exist_ok=True)
    output_image_path = os.path.join(output_dir, "training_curves.png")

    if not os.path.isfile(trainer_state_path):
        print(f"Error: Could not find trainer_state.json at {trainer_state_path}")
        return

    # Load trainer state
    with open(trainer_state_path, "r", encoding="utf-8") as f:
        state = json.load(f)

    log_history = state.get("log_history", [])

    train_steps = []
    train_losses = []
    eval_steps = []
    eval_losses = []

    for entry in log_history:
        step = entry.get("step")
        if "loss" in entry:
            train_steps.append(step)
            train_losses.append(entry["loss"])
        if "eval_loss" in entry:
            eval_steps.append(step)
            eval_losses.append(entry["eval_loss"])

    print(f"Loaded {len(train_losses)} training log points and {len(eval_losses)} validation log points.")

    # Create the plot
    plt.figure(figsize=(10, 6), dpi=200)
    
    # Plot training loss
    plt.plot(train_steps, train_losses, label="Training Loss", color="#1f77b4", alpha=0.6, linewidth=1.5)
    
    # Plot validation loss
    if eval_losses:
        plt.plot(eval_steps, eval_losses, label="Validation Loss", color="#ff7f0e", marker="o", linewidth=2.0)
        
    plt.title("LoRA Fine-tuning Loss Curves", fontsize=14, fontweight="bold", pad=15)
    plt.xlabel("Training Steps", fontsize=11, labelpad=10)
    plt.ylabel("Loss", fontsize=11, labelpad=10)
    plt.grid(True, linestyle="--", alpha=0.5)
    plt.legend(fontsize=11)
    
    # Beautify graph style
    plt.tight_layout()
    
    # Save image
    plt.savefig(output_image_path, bbox_inches="tight")
    plt.close()
    
    print(f"Successfully saved training curves chart to {output_image_path}")

if __name__ == "__main__":
    main()
