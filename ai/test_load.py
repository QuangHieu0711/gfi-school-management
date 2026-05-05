from transformers import AutoConfig, AutoModelForCausalLM
import torch
print("Loading config...")
config = AutoConfig.from_pretrained('Qwen/Qwen2.5-1.5B-Instruct')
print("Config loaded. Initializing empty model...")
model = AutoModelForCausalLM.from_config(config)
print("Empty model initialized!")
