import os
from PIL import Image, ImageDraw

def create_icons(input_path, output_dir):
    try:
        img = Image.open(input_path).convert("RGBA")
        width, height = img.size
        
        # Crop to square
        if width > height:
            left = (width - height) / 2
            top = 0
            right = (width + height) / 2
            bottom = height
            img = img.crop((left, top, right, bottom))
        elif height > width:
            left = 0
            top = (height - width) / 2
            right = width
            bottom = (height + width) / 2
            img = img.crop((left, top, right, bottom))
            
        sizes = {
            "mipmap-mdpi": 48,
            "mipmap-hdpi": 72,
            "mipmap-xhdpi": 96,
            "mipmap-xxhdpi": 144,
            "mipmap-xxxhdpi": 192,
        }
        
        for folder, size in sizes.items():
            folder_path = os.path.join(output_dir, folder)
            os.makedirs(folder_path, exist_ok=True)
            
            # Normal square icon
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
            resized_img.save(os.path.join(folder_path, "ic_launcher.png"))
            
            # Round icon
            mask = Image.new('L', (size, size), 0)
            draw = ImageDraw.Draw(mask)
            draw.ellipse((0, 0, size, size), fill=255)
            
            round_img = resized_img.copy()
            round_img.putalpha(mask)
            round_img.save(os.path.join(folder_path, "ic_launcher_round.png"))
            
            print(f"Generated {size}x{size} icons in {folder}")
            
    except Exception as e:
        print(f"Error generating icons: {e}")

if __name__ == "__main__":
    input_image = r"C:\Users\user\.gemini\antigravity\brain\6226f791-e7f2-4c14-8506-8f31bde12e21\media__1778270808086.jpg"
    res_dir = r"d:\mobilproje\var\app\src\main\res"
    create_icons(input_image, res_dir)
