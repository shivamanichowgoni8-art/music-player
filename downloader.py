import sys
import os
import json

def download(url, output_dir):
    try:
        import yt_dlp
    except ImportError:
        print(json.dumps({"error": "yt-dlp not installed"}))
        sys.exit(1)

    os.makedirs(output_dir, exist_ok=True)

    ydl_opts = {
        'format': 'bestaudio/best',
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }],
        'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Extract info first
            info = ydl.extract_info(url, download=True)
            if not info:
                print(json.dumps({"error": "Failed to extract audio info"}))
                sys.exit(1)

            base_file = ydl.prepare_filename(info)
            base_name, _ = os.path.splitext(base_file)
            mp3_file = base_name + '.mp3'

            result = {
                "success": True,
                "title": info.get("title") or info.get("track") or "Downloaded Track",
                "artist": info.get("artist") or info.get("creator") or info.get("uploader") or "Online Audio",
                "album": info.get("album") or "Y2Music Download",
                "duration": int(info.get("duration") or 180),
                "filename": os.path.basename(mp3_file),
                "filepath": mp3_file
            }
            print(json.dumps(result))
            sys.exit(0)
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Usage: python downloader.py <url> <output_dir>"}))
        sys.exit(1)
    url = sys.argv[1]
    out_dir = sys.argv[2]
    download(url, out_dir)
