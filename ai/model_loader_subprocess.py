"""
Subprocess-based model loader to isolate segfaults and prevent API crashes.
If the model fails to load (e.g., segfault), returns False so generate_comment
can return a safe fallback without crashing the entire API.
"""
import os
import sys
import json
import tempfile
from pathlib import Path


def load_model_in_subprocess(base_model: str, adapter_path: str, timeout_sec: int = 120) -> bool:
    """
    Attempt to load the model in a subprocess. Returns True if successful, False otherwise.
    If a segfault or any other error occurs, the subprocess dies cleanly and we return False.
    """
    import subprocess
    
    loader_script = """
import sys
import os
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

try:
    base_model = sys.argv[1]
    adapter_path = sys.argv[2]
    
    # Suppress HF warnings
    os.environ['HF_HUB_DISABLE_TELEMETRY'] = '1'
    
    # Try to load with conservative memory settings
    tokenizer = AutoTokenizer.from_pretrained(base_model, trust_remote_code=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token
    
    # Load base model with CPU-friendly settings
    base_model_obj = AutoModelForCausalLM.from_pretrained(
        base_model,
        trust_remote_code=True,
        low_cpu_mem_usage=False,  # Try False first since True was causing issues
    )
    
    # Load LoRA adapter
    model = PeftModel.from_pretrained(base_model_obj, adapter_path)
    model.eval()
    
    # Success
    print("SUCCESS", file=sys.stderr)
    sys.exit(0)
except Exception as e:
    print(f"ERROR: {e}", file=sys.stderr)
    sys.exit(1)
"""
    
    try:
        with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
            f.write(loader_script)
            temp_script = f.name
        
        try:
            result = subprocess.run(
                [sys.executable, temp_script, base_model, adapter_path],
                capture_output=True,
                text=True,
                timeout=timeout_sec,
            )
            
            # Check stderr for SUCCESS marker
            if "SUCCESS" in result.stderr and result.returncode == 0:
                return True
            else:
                # Model failed to load (segfault, OOM, or other error)
                if result.stderr:
                    print(f"[model_loader] Subprocess stderr: {result.stderr[:200]}", file=sys.stderr)
                return False
                
        finally:
            try:
                os.unlink(temp_script)
            except:
                pass
                
    except subprocess.TimeoutExpired:
        print(f"[model_loader] Subprocess timed out after {timeout_sec}s", file=sys.stderr)
        return False
    except Exception as e:
        print(f"[model_loader] Failed to run loader subprocess: {e}", file=sys.stderr)
        return False


if __name__ == "__main__":
    # Quick test
    import model_service
    success = load_model_in_subprocess(
        model_service.BASE_MODEL,
        model_service.ADAPTER_PATH,
    )
    print(f"Load result: {success}")
