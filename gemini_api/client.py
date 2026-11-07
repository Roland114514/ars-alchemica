"""
Gemini API Client Module
Direct REST client requiring zero third-party dependencies.
Supports Gemini 4 Argon, Gemini 3.8 Flash, and other current Gemini models.
"""

import os
import json
import ssl
import urllib.request
import urllib.error
from pathlib import Path
from typing import Optional, List, Dict, Any, Union


def load_env(env_path: Optional[Union[str, Path]] = None) -> Dict[str, str]:
    """Simple .env parser without external dependencies."""
    if env_path is None:
        env_path = Path(__file__).resolve().parent.parent / ".env"
    else:
        env_path = Path(env_path)

    env_vars: Dict[str, str] = {}
    if not env_path.exists():
        return env_vars

    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                key, val = line.split("=", 1)
                key = key.strip()
                val = val.strip().strip("\"'").strip()
                env_vars[key] = val
                # Also set in os.environ if not already present
                if key not in os.environ:
                    os.environ[key] = val

    return env_vars


def get_ssl_context() -> ssl.SSLContext:
    """Create a resilient SSL context that handles macOS certificate issues."""
    try:
        import certifi  # type: ignore
        ctx = ssl.create_default_context(cafile=certifi.where())
        return ctx
    except Exception:
        pass

    try:
        ctx = ssl.create_default_context()
        # Test if default context works
        return ctx
    except Exception:
        pass

    # Fallback to unverified context if system certificates are broken
    ctx = ssl._create_unverified_context()
    return ctx


class GeminiClient:
    """Client for calling Google Gemini API directly via REST."""

    DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    def __init__(self, api_key: Optional[str] = None, default_model: str = "gemini-4-argon"):
        load_env()
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY")
        if not self.api_key:
            raise ValueError(
                "Gemini API Key not found. Please provide api_key or set GEMINI_API_KEY in .env"
            )
        self.default_model = default_model
        self.ssl_context = get_ssl_context()

    def generate_content(
        self,
        prompt: Union[str, List[Dict[str, Any]]],
        model: Optional[str] = None,
        system_instruction: Optional[str] = None,
        temperature: float = 0.7,
        max_output_tokens: Optional[int] = None,
        timeout: int = 300,
    ) -> str:
        """
        Generate content using Gemini model.

        :param prompt: Text prompt or list of content parts.
        :param model: Target model name (default: gemini-4-argon).
        :param system_instruction: System prompt/instruction.
        :param temperature: Sampling temperature (0.0 to 2.0).
        :param max_output_tokens: Token limit for output.
        :param timeout: HTTP request timeout in seconds.
        :return: Generated text string.
        """
        model = model or self.default_model
        # Strip 'models/' prefix if provided
        if model.startswith("models/"):
            model = model[len("models/"):]

        url = f"{self.DEFAULT_BASE_URL}/models/{model}:generateContent?key={self.api_key}"

        # Build contents payload
        if isinstance(prompt, str):
            contents = [{"parts": [{"text": prompt}]}]
        elif isinstance(prompt, list):
            contents = prompt
        else:
            raise TypeError("prompt must be a string or list of content dicts")

        payload: Dict[str, Any] = {
            "contents": contents,
            "generationConfig": {
                "temperature": temperature,
            },
        }

        if max_output_tokens:
            payload["generationConfig"]["maxOutputTokens"] = max_output_tokens

        if system_instruction:
            payload["systemInstruction"] = {
                "parts": [{"text": system_instruction}]
            }

        body = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        try:
            with urllib.request.urlopen(req, context=self.ssl_context, timeout=timeout) as response:
                result = json.loads(response.read().decode("utf-8"))

            candidates = result.get("candidates", [])
            if not candidates:
                prompt_feedback = result.get("promptFeedback", {})
                block_reason = prompt_feedback.get("blockReason")
                if block_reason:
                    raise RuntimeError(f"Request blocked by safety filter: {block_reason}")
                return ""

            parts = candidates[0].get("content", {}).get("parts", [])
            output_text = "".join(p.get("text", "") for p in parts)
            return output_text

        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="replace")
            try:
                err_json = json.loads(err_body)
                err_msg = err_json.get("error", {}).get("message", err_body)
            except Exception:
                err_msg = err_body
            raise RuntimeError(f"Gemini API HTTP Error {e.code}: {err_msg}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(f"Network error connecting to Gemini API: {e.reason}") from e

    def list_models(self) -> List[str]:
        """Fetch list of available models for this API key."""
        url = f"{self.DEFAULT_BASE_URL}/models?key={self.api_key}"
        req = urllib.request.Request(url)
        try:
            with urllib.request.urlopen(req, context=self.ssl_context, timeout=30) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return [m["name"].replace("models/", "") for m in data.get("models", [])]
        except Exception as e:
            raise RuntimeError(f"Failed to list models: {e}") from e
