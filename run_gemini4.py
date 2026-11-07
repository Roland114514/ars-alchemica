#!/usr/bin/env python3
"""
CLI runner for Gemini 4 Argon.
Usage:
  python3 run_gemini4.py -p "你的提示词"
  python3 run_gemini4.py -f prompt.txt -o result.md
  python3 run_gemini4.py --interactive
  python3 run_gemini4.py --list-models
"""

import sys
import argparse
from pathlib import Path
from gemini_api.client import GeminiClient


def main():
    parser = argparse.ArgumentParser(
        description="Gemini 4 Modding Assistant Runner"
    )
    parser.add_argument(
        "-p", "--prompt",
        type=str,
        help="Prompt text to send to Gemini 4"
    )
    parser.add_argument(
        "-f", "--file",
        type=str,
        help="Path to file containing prompt text"
    )
    parser.add_argument(
        "-o", "--out",
        type=str,
        help="Path to write the output result"
    )
    parser.add_argument(
        "-s", "--system",
        type=str,
        help="System instruction / role definition"
    )
    parser.add_argument(
        "-m", "--model",
        type=str,
        default="gemini-4-argon",
        help="Model name (default: gemini-4-argon)"
    )
    parser.add_argument(
        "-t", "--temperature",
        type=float,
        default=0.7,
        help="Temperature (default: 0.7)"
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="HTTP request timeout in seconds (default: 300)"
    )
    parser.add_argument(
        "--list-models",
        action="store_true",
        help="List all accessible models"
    )
    parser.add_argument(
        "-i", "--interactive",
        action="store_true",
        help="Enter interactive chat session"
    )

    args = parser.parse_args()

    try:
        client = GeminiClient()
    except Exception as e:
        print(f"[Error] Initialization failed: {e}", file=sys.stderr)
        sys.exit(1)

    if args.list_models:
        print(f"Fetching accessible models for your API key...")
        models = client.list_models()
        print("\nAvailable models:")
        for m in sorted(models):
            print(f"  - {m}")
        return

    if args.interactive or (not args.prompt and not args.file and sys.stdin.isatty()):
        print("=" * 60)
        print(f"🤖 Gemini 4 交互终端 (Model: {args.model})")
        print("输入你的问题或指令，输入 'exit' 或 'quit' 退出。")
        print("=" * 60)

        history = []
        while True:
            try:
                user_input = input("\n[You] > ").strip()
                if not user_input:
                    continue
                if user_input.lower() in ("exit", "quit", "q"):
                    print("退出交互。")
                    break

                history.append({"role": "user", "parts": [{"text": user_input}]})
                print(f"[{args.model} 思考生成中...]")
                resp = client.generate_content(
                    prompt=history,
                    model=args.model,
                    system_instruction=args.system or "你是一个顶尖的Minecraft Mod架构师与Java专家，协助用户开发超大型Minecraft模组。",
                    temperature=args.temperature,
                )
                print(f"\n[Gemini 4]\n{resp}")
                history.append({"role": "model", "parts": [{"text": resp}]})

            except KeyboardInterrupt:
                print("\n操作中断。")
                break
            except Exception as e:
                print(f"[Error] {e}")
        return

    # Handle file or prompt input
    prompt_text = ""
    if args.file:
        file_path = Path(args.file)
        if not file_path.exists():
            print(f"[Error] File not found: {args.file}", file=sys.stderr)
            sys.exit(1)
        prompt_text = file_path.read_text(encoding="utf-8")
        if args.prompt:
            prompt_text = f"{args.prompt}\n\n{prompt_text}"
    elif args.prompt:
        prompt_text = args.prompt
    elif not sys.stdin.isatty():
        prompt_text = sys.stdin.read().strip()

    if not prompt_text:
        parser.print_help()
        sys.exit(1)

    try:
        response_text = client.generate_content(
            prompt=prompt_text,
            model=args.model,
            system_instruction=args.system,
            temperature=args.temperature,
            timeout=args.timeout,
        )

        if args.out:
            out_path = Path(args.out)
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_text(response_text, encoding="utf-8")
            print(f"[Success] Output written to {out_path}")
        else:
            print(response_text)

    except Exception as e:
        print(f"[Error] Generation failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
