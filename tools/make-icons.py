from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[1]/'app/src/main/res'
def icon(size):
    im=Image.new('RGBA',(size*4,size*4),'#0D1015');d=ImageDraw.Draw(im);p=lambda x:int(x*size*4/144)
    d.rounded_rectangle((p(20),p(27),p(124),p(111)),radius=p(19),fill='#171B22',outline='#FFB547',width=p(5))
    d.polygon([(p(61),p(48)),(p(61),p(90)),(p(95),p(69))],fill='#FFB547')
    d.rounded_rectangle((p(57),p(119),p(87),p(123)),radius=p(2),fill='#FFB547')
    return im.resize((size,size),Image.Resampling.LANCZOS)
for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144)]:
    folder=root/f'drawable-{density}';folder.mkdir(parents=True,exist_ok=True);icon(size).save(folder/'ic_launcher.png')
banner=Image.new('RGB',(320,180),'#0D1015');banner.paste(icon(96),(25,42),icon(96));d=ImageDraw.Draw(banner)
d.text((130,66),'HomeTube',font=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',29),fill='#F2F4F7')
d.text((132,104),'VÂN TRƯỜNG',font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',12),fill='#FFB547')
banner.save(root/'drawable-xhdpi/ic_banner.png')
