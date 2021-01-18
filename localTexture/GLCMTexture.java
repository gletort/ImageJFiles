package texter;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import ij.plugin.PlugIn;
import ij.text.*;
import ij.measure.ResultsTable;

public class GLCMTexture
{
	double angle = 0;
	double step = 1;

	public GLCMTexture( double ang, double ste )
	{
		angle = ang;
		step = ste;
	}

	public double calcTexture(ImageProcessor ip, Roi roi, String name )
	{
		double[][] glcm = buildGLCM(ip, roi);
		return getGLCMFeatures( glcm, name );
	}

	public double[][] buildGLCM(ImageProcessor ip, Roi roi)
	{
		double[][] glcm = new double[256][256];
		int dx = (int) (step * Math.cos(angle));
		int dy = (int) (step * Math.sin(angle));
		int i, j;
		int pixelCount = 0;

		Rectangle rec = roi.getBounds();
		// fill matrix
		for ( int x = rec.x; x< (rec.x+rec.width); x++ )
		{
			for ( int y = rec.y; y < (rec.y+rec.height); y++ )
			{
				if ( roi.contains(x,y) && roi.contains(x+dx, y+dy) )
				{
					if ( ip.getPixel(x,y) > 0 )
					{
						i = 0xff & ip.getPixel(x, y);
						j = 0xff & ip.getPixel( x+dx, y+dy);
						glcm [i][j]++;		  			
						glcm [j][i]++;		  			
						pixelCount+=2;
					}
				}
			}
		}

		// convert to proba
		for ( int a = 0; a < 256; a++ )
		{
			for (int b=0; b < 256; b++ )
			{
				glcm[a][b] /= pixelCount;
			}
		}
		return glcm;
	}
	
	// calculate the sum of all glcm elements
	public double getGLCMFeatures(double[][] glcm, String name)
	{
		double asm = 0; // homogeneity
		double entropy = 0.0; //disorder/complexity
		double idm = 0.0; // inverse difference moment, image smoothness
		double clus = 0.0; // cluster tendency (grouping of pixels of same vals)
		double shade = 0.0; // cluster shade (skewness, asymetry of image)
		double correlation = 0.0; // correlation
		double contrast = 0; // contrast or local variation
		double homogen = 0.0; // homogeneity, closeness of elements distributions in GLCM to diagonal
		double variance = 0.0; // variance, graly levels spreading

		double[] stats = getMeanDev(glcm);

		for (int i=0; i<256; i++)  
		{
			for (int j=0; j<256; j++) 
			{
				asm += glcm[i][j]*glcm[i][j];
				if ( glcm[i][j] !=0 )
					entropy -= glcm[i][j]*Math.log(glcm[i][j]);
				idm += ((1.0/(1.0+(Math.pow(i-j,2))))*glcm[i][j]);
				clus += Math.pow(i+j-stats[0]-stats[1], 2) * glcm[i][j];
				shade += Math.pow(i+j-stats[0]-stats[1], 3) * glcm[i][j];
				correlation += i*j*glcm[i][j];
				contrast += Math.pow(i-j, 2) * glcm[i][j];
				homogen += glcm[i][j] / Math.pow(1+Math.abs(i-j),2);
				variance += Math.pow(i-stats[0], 2)*glcm[i][j] + Math.pow(j-stats[1],2)*glcm[i][j];
			}
		}

		correlation = (correlation - stats[0]*stats[1])/(stats[2]*stats[3]);
		
		if ( name.equals("GLCMAngular2Moment") ) return asm;
		if ( name.equals("GLCMEntropy") ) return entropy;
		if ( name.equals("GLCMInverseDiffMoment") ) return idm;
		if ( name.equals("GLCMClusterTendency") ) return clus;
		if ( name.equals("GLCMClusterShade") ) return shade;
		if ( name.equals("GLCMCorrelation") ) return correlation;
		if ( name.equals("GLCMContrast") ) return contrast;
		if ( name.equals("GLCMHomogeneity") ) return homogen;
		if ( name.equals("GLCMVariance") ) return variance;
		return 0;
	}
	


	public double[] getMeanDev(double[][] glcm)
	{
		double [] px = new double [256];
		double [] py = new double [256];
		double[] res = new double[4];
		res[0] = 0.0; //meanx
		res[1] = 0.0; // meany
		res[2] = 0.0; //stdx
		res[3] = 0.0; //stdy

		// First, initialize the arrays to 0
		for (int i = 0;  i < 256; i++)
		{
			px[i] = 0.0;
			py[i] = 0.0;
		}

		// sum the glcm rows to Px(i)
		for (int i = 0;  i < 256; i++) 
		{
			for (int j = 0; j < 256; j++) 
			{
				px[i] += glcm [i][j];
			}
			res[0] += (i*px[i]);
		}

		// sum the glcm rows to Py(j)
		for (int j = 0;  j < 256; j++) 
		{
			for (int i = 0; i < 256; i++) 
			{
				py[j] += glcm [i][j];
			}
			res[1] += j*py[j];
		}

		// calculate stdx and stdy
		for (int i = 0;  i < 256; i++) 
		{
			res[2] += ((Math.pow((i-res[0]),2)) * px[i]);
			res[3] += ((Math.pow((i-res[1]),2)) * py[i]);
		}

		return res;
	}

}
