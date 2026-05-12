import os
import paramiko
import sys

# ══════════════════════════════════════════════════════════
# 🚀 GNHOPPER DEPLOYMENT SCRIPT
# 📝 Automatic plugin JAR upload to server after build
# ══════════════════════════════════════════════════════════

def load_env():
    """Simple .env loader to avoid external dependencies"""
    env_path = os.path.join(os.path.dirname(__file__), "..", ".env")
    if not os.path.exists(env_path):
        return
    
    with open(env_path, "r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            key, value = line.split("=", 1)
            os.environ[key.strip()] = value.strip()

# Load environment variables
load_env()

# === CONFIGURATION ===
HOST = os.getenv("SFTP_HOST", "orbit.pikamc.vn")
PORT = int(os.getenv("SFTP_PORT", 2022))
USERNAME = os.getenv("SFTP_USERNAME")
PASSWORD = os.getenv("SFTP_PASSWORD")

# Local and remote paths
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
LOCAL_JAR = os.path.join(BASE_DIR, "bootstrap", "paper", "build", "libs", "gnhopper.jar")
REMOTE_PATH = "/plugins/gnhopper.jar"

def create_sftp_client():
    if not USERNAME or not PASSWORD:
        print("[ERROR] SFTP_USERNAME or SFTP_PASSWORD not set in .env")
        sys.exit(1)
        
    try:
        transport = paramiko.Transport((HOST, PORT))
        transport.connect(username=USERNAME, password=PASSWORD)
        sftp = paramiko.SFTPClient.from_transport(transport)
        return sftp, transport
    except Exception as e:
        print(f"[ERROR] Connection failed: {e}")
        sys.exit(1)

def main():
    if not os.path.exists(LOCAL_JAR):
        print(f"[ERROR] JAR file not found at: {LOCAL_JAR}")
        print("Please run the build command first!")
        sys.exit(1)

    print(f"Connecting to {HOST}:{PORT}...")
    sftp, transport = create_sftp_client()
    print("Connected!")
    
    try:
        print(f"[SYNC] Uploading: {LOCAL_JAR} -> {REMOTE_PATH}")
        sftp.put(LOCAL_JAR, REMOTE_PATH)
        print("DONE: Upload completed successfully!")
    except Exception as e:
        print(f"[ERROR] Upload failed: {e}")
        sys.exit(1)
    finally:
        sftp.close()
        transport.close()

if __name__ == "__main__":
    main()
