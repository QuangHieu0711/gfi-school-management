---
library_name: peft
license: other
base_model: Qwen/Qwen2.5-1.5B-Instruct
tags:
- base_model:adapter:Qwen/Qwen2.5-1.5B-Instruct
- llama-factory
- lora
- transformers
pipeline_tag: text-generation
model-index:
- name: gfi_comments_lora
  results: []
---

<!-- This model card has been generated automatically according to the information the Trainer had access to. You
should probably proofread and complete it, then remove this comment. -->

# gfi_comments_lora

This model is a fine-tuned version of [Qwen/Qwen2.5-1.5B-Instruct](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct) on the gfi_comments_train dataset.
It achieves the following results on the evaluation set:
- Loss: 0.1296

## Model description

More information needed

## Intended uses & limitations

More information needed

## Training and evaluation data

More information needed

## Training procedure

### Training hyperparameters

The following hyperparameters were used during training:
- learning_rate: 0.0002
- train_batch_size: 1
- eval_batch_size: 1
- seed: 42
- distributed_type: multi-GPU
- num_devices: 2
- gradient_accumulation_steps: 8
- total_train_batch_size: 16
- total_eval_batch_size: 2
- optimizer: Use OptimizerNames.ADAMW_TORCH_FUSED with betas=(0.9,0.999) and epsilon=1e-08 and optimizer_args=No additional optimizer arguments
- lr_scheduler_type: cosine
- lr_scheduler_warmup_steps: 0.05
- num_epochs: 3
- mixed_precision_training: Native AMP

### Training results

| Training Loss | Epoch  | Step | Validation Loss |
|:-------------:|:------:|:----:|:---------------:|
| 0.6796        | 0.2432 | 200  | 0.6717          |
| 0.3612        | 0.4863 | 400  | 0.4065          |
| 0.3013        | 0.7295 | 600  | 0.2891          |
| 0.2511        | 0.9726 | 800  | 0.2324          |
| 0.1661        | 1.2152 | 1000 | 0.2064          |
| 0.1576        | 1.4584 | 1200 | 0.1826          |
| 0.1504        | 1.7015 | 1400 | 0.1628          |
| 0.1515        | 1.9447 | 1600 | 0.1473          |
| 0.1189        | 2.1872 | 1800 | 0.1407          |
| 0.1156        | 2.4304 | 2000 | 0.1347          |
| 0.1148        | 2.6736 | 2200 | 0.1317          |
| 0.1046        | 2.9167 | 2400 | 0.1297          |


### Framework versions

- PEFT 0.18.1
- Transformers 5.0.0
- Pytorch 2.10.0+cu128
- Datasets 4.0.0
- Tokenizers 0.22.2