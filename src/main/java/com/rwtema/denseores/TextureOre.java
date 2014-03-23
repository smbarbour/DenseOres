package com.rwtema.denseores;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;

import scala.reflect.internal.Trees.Modifiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

// Custom texture class to handle the ore generation
public class TextureOre extends TextureAtlasSprite {

	private int n;

	private ResourceLocation textureLocation;

	private String name;

	private String base;

	public static String getDerivedName(String par1Str) {
		return DenseOresMod.MODID + ":" + par1Str + "_dense";
	}

	public TextureOre(String par1Str, String base) {
		super(getDerivedName(par1Str));
		this.name = par1Str;
		this.base = base;
	}

	// should we use a custom loader to get our texture?
	public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
		try {
			// check to see if the resource can be loaded (someone added an
			// override)
			manager.getResource(location);
		} catch (IOException e) {
			// file not found: let's generate one
			return true;
		}

		System.out.println("Dense Ores: Detected override for " + name);
		return false;
	}

	// converts texture name to resource location
	public ResourceLocation getBlockResource(String s2) {
		String s1 = "minecraft";

		int ind = s2.indexOf(58);

		if (ind >= 0) {
			if (ind > 1) {
				s1 = s2.substring(0, ind);
			}

			s2 = s2.substring(ind + 1, s2.length());
		}

		s1 = s1.toLowerCase();
		s2 = "textures/blocks/" + s2 + ".png";
	;
		return new ResourceLocation(s1, s2);
	}

	// loads the textures
	// note: the documentation
	/**
	 * Load the specified resource as this sprite's data. Returning false from
	 * this function will prevent this icon from being stitched onto the master
	 * texture.
	 * 
	 * @param manager
	 *            Main resource manager
	 * @param location
	 *            File resource location
	 * @return False to prevent this Icon from being stitched
	 */
	// is not correct - return TRUE to prevent this Icon from being stitched
	// (makes no sense but... whatever)

	// this code is based on code from TextureMap.loadTextureAtlas, only with
	// the
	// code for custom mip-mapping textures and animation removed.
	// TODO: add animation support
	// TODO: add check for textures being of different sizes

	public boolean load(IResourceManager manager, ResourceLocation location) {

		// get mipmapping level
		int mp = Minecraft.getMinecraft().gameSettings.mipmapLevels;

		try {
			IResource iresource = manager.getResource(getBlockResource(name));
			IResource iresourceBase = manager.getResource(getBlockResource(base));

			// creates a buffer that will be used for our texture and the
			// various mip-maps
			// (mip-mapping is where you use smaller textures when objects are
			// far-away
			// see: http://en.wikipedia.org/wiki/Mipmap)
			// these will be generated from the base texture
			BufferedImage[] ore_image = new BufferedImage[1 + mp];

			// load the ore texture
			ore_image[0] = ImageIO.read(iresource.getInputStream());

			// load the stone texture
			BufferedImage stone_image = ImageIO.read(iresourceBase.getInputStream());

			int w = ore_image[0].getWidth();

			// create an output image that we will use to override
			BufferedImage output_image = new BufferedImage(w, w, ore_image[0].getType());

			if (w != stone_image.getWidth()) {
				return true;
			}

			int[] ore_data = new int[w * w];
			int[] stone_data = new int[w * w];
			int[] new_data = new int[w * w];

			// read the rgb color data into our array
			ore_image[0].getRGB(0, 0, output_image.getWidth(), output_image.getWidth(), ore_data, 0, output_image.getWidth());
			stone_image.getRGB(0, 0, w, w, stone_data, 0, stone_image.getWidth());

			int h = w;

			// check to see which pixels are different

			boolean[] same = new boolean[w * w];
			for (int i = 0; i < ore_data.length; i += 1) {
				same[i] = ore_data[i] == stone_data[i];
				new_data[i] = ore_data[i];
			}

			int dx[] = new int[] { -1, 2, 3 };
			int dy[] = new int[] { -1, 0, 1 };

			// where the magic happens

			for (int i = 0; i < ore_data.length; i += 1) {
				int x = (i % w);
				int y = (i - x) / w;

				// if a pixel is part of the stone texture it should change if
				// possible
				boolean shouldChange = same[i];

				// compare the pixel to its shifted counterparts and change it
				// if the rotated pixel
				// is 'different' from the stone texture and is either brighter
				// or the original pixel
				// was marked as 'shouldChange'.

				for (int j = 0; j < dx.length; j++) {
					if ((x + dx[j]) >= 0 && (x + dx[j]) < w && (y + dy[j]) >= 0 && (y + dy[j]) < w)
						if (!same[(x + dx[j]) + (y + dy[j]) * w] && (shouldChange
						// || lum(new_data[i]) > lum(ore_data[(x +
						// dx[j]) + (y +
						// dy[j]) * w])
								)) {
							shouldChange = false;
							new_data[i] = ore_data[(x + dx[j]) + (y + dy[j]) * w];
						}
				}

			}

			// write the new image data to the output image buffer
			output_image.setRGB(0, 0, output_image.getWidth(), output_image.getHeight(), new_data, 0, output_image.getWidth());

			// replace the old texture
			ore_image[0] = output_image;

			// load the texture (note the null is where animation data would
			// normally go)

			this.loadSprite(ore_image, null, (float) Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1.0F);
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}

		System.out.println("Dense Ores: Succesfully generated dense ore texture for '" + name + "' with background '" + base + "'. Place " + name + "_dense.png in the assets folder to override.");
		return false;
	}

	// get the lighter of two colors
	public int lighten(int col_a, int col_b) {
		// get rgb values from color
		// note that you need to use -col as the color format is always negative

		int r = Math.min(((-col_a) >> 16 & 255), ((-col_b) >> 16 & 255));
		int g = Math.min(((-col_a) >> 8 & 255), ((-col_b) >> 8 & 255));
		int b = Math.min(((-col_a) & 255), ((-col_b) & 255));

		return -(r << 16 | g << 8 | b);
	}

	// get luminance from color
	public float lum(int col) {
		// get rgb values from color
		// note that you need to use -col as the color format is always negative
		float r = (float) ((-col) >> 16 & 255) / 255.0F;
		float g = (float) ((-col) >> 8 & 255) / 255.0F;
		float b = (float) ((-col) & 255) / 255.0F;
		r = 1 - r;
		g = 1 - g;
		b = 1 - b;

		// combine the colors the get overall luminance
		// fun science fact: the human eye is more sensitive to different colors
		// so its not a simple average
		return r * 0.2126F + g * 0.7152F + b * 0.0722F;
	}
}