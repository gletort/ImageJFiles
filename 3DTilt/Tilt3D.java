package tilt;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.measure.*;
import java.io.*;

public class Tilt3D implements PlugIn 
{

	double tx, ty, rot;
	int nframes, dir;
	private static int interpolationMethod = ImageProcessor.BILINEAR;


	public void getOptions()
	{
		GenericDialog gd = new GenericDialog("Tilt parameters");
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
		gd.addNumericField("Tilt radius in X (px):", 20, 0);
		gd.addNumericField("Tilt radius in Y (px):", -20, 0);
		gd.addNumericField("Rotation angle:", 15, 2);
		gd.addNumericField("Number of movie frames:", 40, 0);
		
		String choices[] = {"Last frame is top", "First frame is top"};
		gd.addChoice( "Z direction:", choices, choices[0] );
		
		gd.showDialog();
		if (gd.wasCanceled()) return;

		tx = gd.getNextNumber();
		ty = gd.getNextNumber();
		rot = gd.getNextNumber();
		nframes = (int) gd.getNextNumber();
		String zdir = gd.getNextChoice();
		if ( zdir.equals( choices[0] ) )
			dir = 0;
		else 
			dir = 1;
	}


	public void run(String arg) 
	{
		IJ.run("Select None");
		getOptions();
		ImagePlus main = IJ.getImage();
		String title = main.getTitle();
		title = title+"_tilt_movie.tif";
		int nchannels = main.getNChannels();

		ImagePlus channels[] = new ImagePlus[nchannels];
		if ( nchannels > 1 )
		{
			channels = ChannelSplitter.split(main);
		}
		else
		{
			channels[0] = main;
		}

		ImageStack stack[] = new ImageStack[nchannels];
		for (int c = 0; c < nchannels; c++ )
		   stack[c]	= new ImageStack( (channels[0]).getWidth(), (channels[0]).getHeight() );
		
		int nSlices = (channels[0]).getNSlices();
		int halfSlices = (int) (nSlices/2.0);
		
		double curtx, curty, curAngle, dx, dy;
		ImageProcessor ip = channels[0].getProcessor();
		ip.setInterpolationMethod(interpolationMethod);
		ZProjector zproj = new ZProjector();
		zproj.setMethod( zproj.MAX_METHOD );

		tx = -4*tx/nframes/nSlices;
		ty = -4*ty/nframes/nSlices;
		rot = rot*(-1)/nframes;

		// movie frames
		for ( int fr = 1; fr <= nframes; fr++ )
		{
			curtx = fr*tx;
			curty = fr*ty;
			curAngle = fr*rot;
		
			for ( int chan = 0; chan < nchannels; chan++ )
			{	
				IJ.showProgress( fr, nframes );
				IJ.showStatus("-------------------- slice "+fr+" / "+nframes);
				ImagePlus dup = new Duplicator().run(channels[chan]);
				ImageStack dupstack = dup.getImageStack();

				for ( int z = 0; z < nSlices; z++ )
				{
				
					dx = (z-halfSlices) * curtx;
					dy = (z-halfSlices) * curty;

					// according to direction, set current slice
					if ( dir <= 0 )
						ip = dupstack.getProcessor(nSlices-z);
					else
						ip = dupstack.getProcessor(z+1);
					
					ip.translate(dx, dy);
				}
		
				// z projection
				zproj.setImage(dup);
				zproj.doProjection();
				// rotation
				ImageProcessor projip = (zproj.getProjection()).getProcessor();
				projip.rotate(curAngle);
				
				dup.changes = false;
				dup.close();

				stack[chan].addSlice(projip);
			}
		}

		ImagePlus istack[] = new ImagePlus[nchannels];
		for ( int c = 0; c < nchannels; c++ )		
		{
			istack[c] = new ImagePlus();
			(istack[c]).setStack(stack[c]);
		}
		ImagePlus res = istack[0];
		if ( nchannels > 1 )
			res = new RGBStackMerge().mergeChannels(istack, false);
		res.setTitle(title);
		res.show();
	}		

}
