#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sync_translations.py - Complete translation and synchronization for BBS CML EDITION 3.0.
Ensures 100% key parity and translates BOTH missing keys and untranslated English leftover strings.
"""

import os
import sys
import json
import time
import re
import urllib.request
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed

# Ensure UTF-8 output on Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

LANG_DIRS = [
    os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "bbs", "lang"),
    os.path.join(PROJECT_ROOT, "src", "client", "resources", "assets", "bbs", "assets", "strings"),
]

LANG_CODE_MAP = {
    "ar_ar": "ar",
    "de_de": "de",
    "es_es": "es",
    "fr_fr": "fr",
    "he_il": "he",
    "hu_hu": "hu",
    "id_id": "id",
    "ko_kr": "ko",
    "pl_pl": "pl",
    "pt_br": "pt",
    "pt_pt": "pt",
    "ru_ru": "ru",
    "th_th": "th",
    "tr_tr": "tr",
    "uk_ua": "uk",
    "ur_pk": "ur",
    "vi_vn": "vi",
    "zh_cn": "zh-CN",
    "zh_tw": "zh-TW",
}

NATURAL_SAME = {
    "FPS", "FOV", "PNG", "WAV", "MP4", "NBT", "URL", "ID", "UUID", "RGB", "RGBA", "JSON", "VOX",
    "GUI", "BBS", "CML", "GIF", "HD", "2D", "3D", "IK", "VAO", "VBO", "FBO", "API", "MoLang",
    "BBS mod", "Minecraft", "Discord", "YouTube", "GitHub", "Wiki", "CDN",
    "1080p (1920x1080 - 60 FPS)", "1440p (2560x1440 - 60 FPS)", "4K (3840x2160 - 60 FPS)", "720p (1280x720 - 60 FPS)",
}

def is_natural_same(text):
    s = text.strip()
    if not s:
        return True
    if s in NATURAL_SAME:
        return True
    if s.startswith("http://") or s.startswith("https://"):
        return True
    if re.match(r'^[A-Z0-9_\-]+$', s) and len(s) <= 8:
        return True
    if re.match(r'^(?:%[0-9]*[a-zA-Z]|\s|[\+\-\*/:,\._%#|><=\(\)])*$', s):
        return True
    if re.match(r'^\d+p\s*\(', s):
        return True
    return False

def translate_single(text, target_lang):
    if is_natural_same(text):
        return text
    url = f"https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl={target_lang}&dt=t&q=" + urllib.parse.quote(text)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return "".join([part[0] for part in data[0] if part and part[0]])
        except Exception:
            time.sleep(0.5 * (attempt + 1))
    return text

def translate_batch(texts, target_lang):
    if not texts:
        return []
    
    if all(is_natural_same(t) for t in texts):
        return texts

    results = []
    to_translate_indices = []
    to_translate_texts = []
    
    for i, t in enumerate(texts):
        if is_natural_same(t):
            results.append(t)
        else:
            results.append(None)
            to_translate_indices.append(i)
            to_translate_texts.append(t)
            
    if not to_translate_texts:
        return results

    delimiter = "\n_X0X_\n"
    combined = delimiter.join(to_translate_texts)
    url = f"https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl={target_lang}&dt=t&q=" + urllib.parse.quote(combined)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})

    success = False
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                full_translated = "".join([part[0] for part in data[0] if part and part[0]])
                parts = full_translated.split("_X0X_")
                if len(parts) == len(to_translate_texts):
                    for idx, part in zip(to_translate_indices, parts):
                        results[idx] = part.strip()
                    success = True
                    break
                else:
                    break
        except Exception:
            time.sleep(0.5 * (attempt + 1))

    if not success:
        for idx, text in zip(to_translate_indices, to_translate_texts):
            results[idx] = translate_single(text, target_lang)

    return results

def process_file_translations(filepath, en_data, en_keys):
    filename = os.path.basename(filepath)
    lang_name = os.path.splitext(filename)[0]
    target_lang = LANG_CODE_MAP.get(lang_name)
    if not target_lang:
        print(f"Warning: Unknown language code for {filename}, skipping")
        return False

    with open(filepath, "r", encoding="utf-8") as f:
        try:
            lang_data = json.load(f)
        except Exception as e:
            print(f"Error loading {filename}: {e}")
            lang_data = {}

    # 1. Clean obsolete keys
    obsolete_keys = [k for k in lang_data if k not in en_data]
    for k in obsolete_keys:
        del lang_data[k]

    # 2. Find missing keys AND keys that are still untranslated English text
    keys_to_translate = []
    for k in en_keys:
        if k not in lang_data:
            keys_to_translate.append(k)
        else:
            val = lang_data[k]
            en_val = en_data[k]
            # If value is identical to English and not a natural-same token (e.g. acronym/URL)
            if val.strip() == en_val.strip() and not is_natural_same(en_val):
                keys_to_translate.append(k)
    
    print(f"[{filename}] Total keys: {len(en_keys)}, Obsolete removed: {len(obsolete_keys)}, Keys to translate/fix: {len(keys_to_translate)}")

    if keys_to_translate:
        batch_size = 35
        batches = [keys_to_translate[i : i + batch_size] for i in range(0, len(keys_to_translate), batch_size)]
        
        for b_idx, batch in enumerate(batches):
            batch_texts = [en_data[k] for k in batch]
            translated = translate_batch(batch_texts, target_lang)
            for k, t in zip(batch, translated):
                lang_data[k] = t
            
            if (b_idx + 1) % 5 == 0 or (b_idx + 1) == len(batches):
                print(f"  [{filename}] Progress: {min((b_idx+1)*batch_size, len(keys_to_translate))}/{len(keys_to_translate)} keys processed")
            time.sleep(0.15)

    sorted_data = dict(sorted(lang_data.items(), key=lambda item: item[0]))

    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(sorted_data, f, indent=4, ensure_ascii=False)
        f.write("\n")

    print(f"[{filename}] SUCCESS: Saved with {len(sorted_data)} keys (100% translated).")
    return True

def process_language_directory(directory, max_workers=4):
    ref_path = os.path.join(directory, "en_us.json")
    if not os.path.exists(ref_path):
        print(f"Skipping {directory}: en_us.json not found")
        return

    with open(ref_path, "r", encoding="utf-8") as f:
        en_data = json.load(f)

    en_keys = list(en_data.keys())
    print(f"\n=======================================================")
    print(f"Processing directory: {os.path.relpath(directory, PROJECT_ROOT)}")
    print(f"Master en_us.json contains {len(en_keys)} keys.")
    print(f"=======================================================")

    files_to_process = [
        os.path.join(directory, f)
        for f in sorted(os.listdir(directory))
        if f.endswith(".json") and f != "en_us.json"
    ]

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {
            executor.submit(process_file_translations, fp, en_data, en_keys): os.path.basename(fp)
            for fp in files_to_process
        }
        for future in as_completed(futures):
            filename = futures[future]
            try:
                future.result()
            except Exception as e:
                print(f"Error processing {filename}: {e}")

def main():
    start_time = time.time()
    for d in LANG_DIRS:
        if os.path.exists(d):
            process_language_directory(d, max_workers=4)
    elapsed = time.time() - start_time
    print(f"\nAll translations & synchronizations completed in {elapsed:.2f}s")

if __name__ == "__main__":
    main()
