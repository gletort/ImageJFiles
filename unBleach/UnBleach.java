/** 
 * \brief Plugin to align (translate and rotate) images based on their intensity moments
 *
 * \details 
 * 
 *
 * \author G. Letort, College de France
 * \date created on 2019/02/28
 * */

package unbleach;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.measure.*;
import java.io.*;
import java.util.*;
import java.awt.*;

public class UnBleach implements PlugIn 
{
	ImagePlus imp;
	int nbins;

	public void getOptions()
	{
		GenericDialog gd = new GenericDialog("");

		gd.showDialog();
		if (gd.wasCanceled()) return;

	}

	public double[] getCDF(int[] histo)
	{
		int ntot = 0;
		int nb = histo.length;
		for ( int i = 0; i < nb; i++ )
		{
			ntot += histo[i];
		}

		double[] cdf = new double[nb];
		int cumul = 0;
		for ( int j = 0; j < nb; j++ )
		{
			cumul += histo[j];
			cdf[j] = (double) cumul/ntot;
		}
		return cdf;
	}
	
	public void matchHistrograms()
	{
		// Histogram to match to
		imp.setSlice(1);
		ImageProcessor ip = imp.getProcessor();
		int[] refHisto = ip.getHistogram();
		double[] refCdf = getCDF(refHisto);

		// Change histograms of each slice
		for ( int z = 2; z <= imp.getNSlices(); z++ )
		{
			IJ.showStatus("Histogram matching...  "+z+"/"+imp.getNSlices());
			IJ.showProgress(z, imp.getNSlices());	
			imp.setSlice(z);
			ip = imp.getProcessor();
			// current histogram to transform
			int[] curHisto = ip.getHistogram();
			double[] curCDF = getCDF(curHisto);

			// find mapping
			int[] map = new int[nbins];
			for ( int i = 0; i < nbins; i++ )
			{
				int j = 0;
				while (j < nbins && refCdf[j] < curCDF[i] )
				{
					j++;
				}
				// pix i -> map to j+1
				if ( refCdf[j] != curCDF[i] ) j--;
				map[i] = j;
			}
		
			ip.applyTable(map);
			imp.setProcessor(ip);
			imp.updateAndDraw();
		}
	}


	public void run(String arg) 
	{
		imp = IJ.getImage();
		nbins = 0;
		//imp.hide();
		if ( imp.getBitDepth() == 8 ) nbins = 256;
		else
		{
			if ( imp.getBitDepth() == 16 ) nbins = 65536;
			else
			{
				IJ.error("Stack must be 8-bit or 16-bit");
				return;
			}
		}

		matchHistrograms();
		//imp.show();
	}		

}
