"""render_start_java_backend.py

Deploy-friendly entrypoint for Render.

This script starts the Spring Boot backend by invoking Maven.
Why: Render Web/Worker services need ONE start command; this lets you keep
one place for the run logic.

Expected repo layout (this repository):
- back/pom.xml
- back/src/main/java/.../GoAndStudyBackendApplication.java

Usage:
  python render_start_java_backend.py

Environment variables you can set in Render:
- MVN_ARGS (default: "-q -DskipTests")
- MAIN_CLASS (default: com.goandstudybackend.GoAndStudyBackendApplication)
- PORT (Render injects this; Spring Boot should read it if configured)

Notes:
- Render will set PORT automatically.
- Your Spring config should use ${PORT:8080} or Spring should honor server.port
  via `--server.port` if you pass it.
"""

import os
import subprocess
import sys
from pathlib import Path


def main() -> None:
    repo_root = Path(__file__).resolve().parent
    back_dir = repo_root / "back"

    mvn_args = os.getenv("MVN_ARGS", "-q -DskipTests")
    main_class = os.getenv(
        "MAIN_CLASS", "com.goandstudybackend.GoAndStudyBackendApplication"
    )

    # Render sets PORT; pass it to Spring Boot
    port = os.getenv("PORT")
    server_port_arg = f"-Dserver.port={port}" if port else ""

    if not (back_dir / "pom.xml").exists():
        print(f"Error: back/pom.xml not found at {back_dir}", file=sys.stderr)
        sys.exit(1)

    cmd = [
        "mvn",
        *mvn_args.split(),
        "spring-boot:run",
        f"-Dspring-boot.run.main-class={main_class}",
    ]
    if server_port_arg:
        # server_port_arg like "-Dserver.port=1234"
        cmd.append(server_port_arg)

    # Stream logs
    print("Starting Java backend with:")
    print(" ".join(cmd))

    p = subprocess.Popen(
        cmd,
        cwd=str(back_dir),
        env=os.environ.copy(),
    )
    rc = p.wait()
    sys.exit(rc)


if __name__ == "__main__":
    main()

