FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt app.py ./
RUN pip install --no-cache-dir -r requirements.txt
EXPOSE 8000
# Luôn cập nhật yt-dlp khi khởi động để theo kịp thay đổi của YouTube
CMD pip install -U --no-cache-dir yt-dlp >/dev/null 2>&1; python app.py
