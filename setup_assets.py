import os
import urllib.request
from PIL import Image

# 1. Resize and save icon
icon_path = "/root/.gemini/antigravity-cli/brain/3263c40b-e1db-4e59-9b65-0f8ae7140dc5/app_icon_1782064466627.jpg"
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

base_res_dir = "/root/NativeScheduleApp/app/src/main/res"

print("Opening icon:", icon_path)
try:
    with Image.open(icon_path) as img:
        # Convert to RGBA to ensure PNG saving works properly and with transparency if it had any
        img = img.convert("RGBA")
        
        for density, size in sizes.items():
            dir_path = os.path.join(base_res_dir, f"mipmap-{density}")
            os.makedirs(dir_path, exist_ok=True)
            
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
            save_path = os.path.join(dir_path, "ic_launcher.png")
            resized_img.save(save_path, "PNG")
            print(f"Saved {save_path}")
except Exception as e:
    print(f"Error processing image: {e}")

# 2. Download Fonts
font_dir = os.path.join(base_res_dir, "font")
os.makedirs(font_dir, exist_ok=True)

# Using stable URLs from a known CDN or repo for these fonts, or standard weights.
# We will download static fonts to avoid issues with variable fonts in some older Android versions,
# but variable fonts work well in Compose. Let's use githubusercontent for Google Fonts.
fonts = {
    "oswald.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/Oswald%5Bwght%5D.ttf",
    "manrope.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/manrope/Manrope%5Bwght%5D.ttf"
}

for font_name, url in fonts.items():
    save_path = os.path.join(font_dir, font_name)
    print(f"Downloading {font_name} from {url}...")
    try:
        urllib.request.urlretrieve(url, save_path)
        print(f"Saved {save_path}")
    except Exception as e:
        print(f"Error downloading {font_name}: {e}")

print("Done.")
