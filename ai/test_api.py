#!/usr/bin/env python3
"""
Quick test client for the GFI comment API.
"""
import requests
import json
import time
import subprocess
import sys

BASE_URL = "http://127.0.0.1:8001"

def test_health():
    """Test the health endpoint."""
    print("Testing /health...")
    try:
        r = requests.get(f"{BASE_URL}/health", timeout=5)
        print(f"  Status: {r.status_code}")
        print(f"  Response: {r.json()}")
        return r.status_code == 200
    except Exception as e:
        print(f"  Error: {e}")
        return False

def test_generate_comment():
    """Test the generate-comment endpoint."""
    print("\nTesting /generate-comment...")
    payload = {
        "grade_level": "2",
        "subject_name": "Thể dục",
        "term": "1",
        "week_no": 10,
        "lesson_no": 1,
        "lesson_title": "Bài học về phối hợp",
        "learning_objective": "Học sinh biết phối hợp",
        "evaluation": "tốt",
        "attendance_full": 1,
        "participation_level": "tích cực",
        "behavior_tag": "lịch sự",
        "textbook_series": "Chân trời sáng tạo",
    }
    
    try:
        r = requests.post(
            f"{BASE_URL}/generate-comment",
            json=payload,
            timeout=10,
        )
        print(f"  Status: {r.status_code}")
        resp = r.json()
        print(f"  Response: {json.dumps(resp, ensure_ascii=False, indent=2)}")
        return r.status_code == 200
    except Exception as e:
        print(f"  Error: {e}")
        return False

def main():
    print("Starting server in background...")
    proc = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "app:app", "--host", "127.0.0.1", "--port", "8001"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    
    # Wait for server to start
    time.sleep(3)
    
    try:
        success = True
        success &= test_health()
        success &= test_generate_comment()
        
        if success:
            print("\n✓ All tests passed!")
        else:
            print("\n✗ Some tests failed.")
        
        sys.exit(0 if success else 1)
    finally:
        print("\nShutting down server...")
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()

if __name__ == "__main__":
    main()
