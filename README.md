# NewsAI - AI Features

## Overview
NewsAI is an Android news application with AI-first experiences focused on two core flows:
- AI conversational assistance for each article (context-aware chatbot)
- AI-powered fact checking through a multi-step verification pipeline

This README intentionally focuses only on the AI parts of the project.

## AI Features
- Context-aware chatbot:
	- Chat assistant is launched from article context and receives article title and URL.
	- Uses conversation history so replies stay consistent across turns.
	- Generates Vietnamese responses for explanation, summary, and follow-up analysis.

- AI fact-checking pipeline:
	- Runs a staged workflow: search -> extract -> verify.
	- Finds related sources, extracts candidate evidence, then evaluates claim validity.
	- Shows user-facing verification result and source-review progress.

- Smart usage control:
	- Free tier is rate-limited for AI interactions.
	- VIP users get higher/no practical limits for chatbot and verification usage.

- Source-aware prompting:
	- Chat prompt includes article metadata when available.
	- If article context is weak, assistant can use broader knowledge and provide references.

## AI Architecture
- Chat AI service:
	- Implemented in GrokApiService
	- Endpoint: https://api.x.ai/v1/chat/completions
	- Handles prompt construction, multi-turn messages, and async callbacks.

- Verification AI service:
	- Implemented through NewsVerifyApiService + ApiClient
	- Base verification API flow:
		- POST /search
		- POST /extract
		- POST /verify

- Networking stack:
	- Retrofit + OkHttp
	- Moshi/Gson converters

## AI Configuration
1. Configure chatbot key and model settings:
- File: app/src/main/java/com/example/newsai/GrokApiService.java
- Set API key, model, temperature, and max tokens.

2. Configure verification endpoints:
- File: app/src/main/java/com/example/newsai/network/ApiClient.java
- Set Main API base URL and News Verify API base URL.

3. Optional prompt tuning:
- File: app/src/main/java/com/example/newsai/GrokApiService.java
- Update system prompt logic in buildSystemPrompt for your tone and citation policy.

## Run (AI Focus)
1. Build the Android app:

```powershell
.\gradlew.bat assembleDebug
```

2. Install and run on device/emulator:

```powershell
.\gradlew.bat installDebug
```

3. Test AI flows in app:
- Open article detail -> start chatbot -> send multi-turn questions.
- Open Verify News screen -> submit a claim -> observe search/extract/verify stages.

## Security Notes
- Move AI API keys and sensitive tokens out of source code before production.
- Use local.properties, encrypted secrets, or remote secret management.
