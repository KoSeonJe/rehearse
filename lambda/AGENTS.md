# Lambda Agent Instructions

Use this file for all work under `lambda/`. Also read the root `AGENTS.md`.

## Required Reading

Before editing Lambda code, consult:

- `lambda/CLAUDE.md` for the current Lambda-specific project context.
- `docs/architecture/lambda-deployment.md` for deployment, IAM, and environment variable details.
- `docs/architecture/system-flow.md` for the S3 to EventBridge to Lambda to S3 result flow.
- Relevant prompt redesign or prompt improvement specs under `docs/plans/` when modifying analysis prompts or scoring behavior.

## Structure

```text
lambda/
├── analysis/   # video/audio analysis
└── convert/    # MediaConvert trigger
```

Runtime is Python 3.12 on AWS Lambda using Docker image based deployment through ECR.

## AI Stack

Lambda AI usage is separate from backend AI usage:

- Audio integrated analysis: Google Gemini primary path.
- Frame/nonverbal analysis: OpenAI GPT-4o Vision.
- STT fallback: OpenAI Whisper.

Do not assume backend prompt builders, Java code, or backend AI provider rules apply to Lambda. Backend and Lambda communicate through S3/EventBridge-driven flows, not direct imports or direct invocation.

## Deployment

Commands:

```bash
cd lambda
./deploy.sh
./lambda-safe-deploy.sh
```

Use `lambda-safe-deploy.sh` for production-oriented deployment. The safe path publishes a version, runs smoke checks, and updates the alias with rollback behavior when the script supports it.

## Rules

- Do not move the `lambda/` directory or change key paths without a dedicated plan. Many architecture docs and deploy scripts depend on the current layout.
- Do not share imports with backend Java code. Lambda is independently packaged Python.
- Do not directly couple backend and Lambda. Use the documented S3/EventBridge flow.
- Run smoke tests before production deployment.
- Prompt or analysis logic changes require a prior spec in `.omc/plans/` or `docs/plans/`.
- Keep secrets and provider keys out of tracked files and logs.

## Testing and Verification

- Prefer local Docker/SAM or container-based checks when changing runtime behavior.
- Validate handler entry points, environment variables, and payload shapes against architecture docs.
- For deployment-sensitive changes, inspect the relevant AWS configuration instead of relying only on docs.
