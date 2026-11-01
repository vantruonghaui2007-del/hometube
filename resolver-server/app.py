"""
HomeKara – máy chủ phân giải dự phòng (chạy trên PC/NAS trong nhà).
Chỉ dùng khi box không tự lấy được link YouTube. Video vẫn tải thẳng từ YouTube về box.

Chạy:  pip install -r requirements.txt && python app.py
Rồi trên box: Cài đặt › Máy chủ phân giải = http://<IP-máy-này>:8000
"""
from fastapi import FastAPI, Query
from fastapi.responses import JSONResponse
import yt_dlp

app = FastAPI(title="HomeKara resolver")

BASE_OPTS = {"quiet": True, "no_warnings": True, "skip_download": True, "noplaylist": True}


def kind_of(f):
    v = f.get("vcodec") or "none"
    a = f.get("acodec") or "none"
    if v != "none" and a != "none":
        return "PROGRESSIVE"
    if v != "none":
        return "VIDEO_ONLY"
    if a != "none":
        return "AUDIO_ONLY"
    return None


@app.get("/resolve")
def resolve(v: str = Query(..., min_length=11, max_length=11)):
    try:
        with yt_dlp.YoutubeDL(BASE_OPTS) as ydl:
            info = ydl.extract_info(f"https://www.youtube.com/watch?v={v}", download=False)
    except yt_dlp.utils.DownloadError as e:
        msg = str(e).lower()
        if "age" in msg:
            reason = "Video giới hạn độ tuổi"
        elif "country" in msg or "region" in msg:
            reason = "Video bị chặn tại Việt Nam"
        else:
            reason = "Video không còn tồn tại hoặc không xem được"
        return JSONResponse({"reason": reason}, status_code=422)
    if info.get("is_live"):
        return JSONResponse({"reason": "Không hát được video đang phát trực tiếp"}, status_code=422)
    out = []
    for f in info.get("formats") or []:
        k = kind_of(f)
        if not k or not f.get("url") or (f.get("protocol") or "").startswith(("m3u8", "http_dash")):
            continue
        codec = (f.get("vcodec") if k != "AUDIO_ONLY" else f.get("acodec")) or ""
        out.append({
            "url": f["url"], "kind": k, "codec": codec,
            "height": int(f.get("height") or 0),
            "bitrate": int((f.get("tbr") or 0) * 1000),
        })
    return {"candidates": out}


@app.get("/search")
def search(q: str, page: int = 1):
    n = 20 * max(1, min(page, 5))
    opts = dict(BASE_OPTS, extract_flat=True)
    with yt_dlp.YoutubeDL(opts) as ydl:
        res = ydl.extract_info(f"ytsearch{n}:{q}", download=False)
    entries = (res.get("entries") or [])[n - 20:]
    songs = []
    for e in entries:
        vid = e.get("id")
        if not vid or len(vid) != 11:
            continue
        songs.append({
            "videoId": vid, "title": e.get("title") or "", "channel": e.get("channel") or e.get("uploader") or "",
            "durationSec": int(e.get("duration") or 0),
            "thumbnailUrl": f"https://i.ytimg.com/vi/{vid}/mqdefault.jpg",
        })
    return {"songs": songs, "hasMore": page < 5 and len(entries) >= 20}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
