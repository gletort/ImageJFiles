/** 
 * \brief Plugin to calculte GLCM texture on moving window.
 *
 *
 * \author G. Letort, College de France
 * \date created on 2021/01
 * */

package texter;
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

public class LocalTexture implements PlugIn
{
	ImagePlus imp, itext;
	int wsize=20;
	int step=5;
	double angle = 0;
	double glcmstep = 1;
	String texture;

	/** Be sure there s no calibration */
	public void initCalibration()
	{
		// Measure everything in pixels
		Calibration cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;
	}

	
	/** \brief Dialog window */
	public boolean getParameters()
	{

		GenericDialog gd = new GenericDialog("Options", IJ.getInstance() );
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
		gd.addNumericField("window_size", wsize, 0);
		gd.addNumericField("window_step", step, 0);
		String[] textures = new String[9];
		textures[0] = "GLCMAngular2Moment";
		textures[1] = "GLCMEntropy";
		textures[2] = "GLCMInverseDiffMoment";
		textures[3] = "GLCMClusterTendency";
		textures[4] = "GLCMClusterShade";
		textures[5] = "GLCMCorrelation";
		textures[6] = "GLCMContrast";
		textures[7] = "GLCMHomogeneity";
		textures[8] = "GLCMVariance";
		gd.addChoice("Texture", textures, textures[1]);
		gd.addNumericField("GLCM_angle", angle, 2);
		gd.addNumericField("GLCM_step", glcmstep, 1);
		gd.showDialog();
		if (gd.wasCanceled()) return false;

		wsize = (int) gd.getNextNumber();
		step = (int) gd.getNextNumber();
		texture = gd.getNextChoice();
		angle = gd.getNextNumber();
		glcmstep = gd.getNextNumber();
		return true;
	}

	public void getTexture()
	{
		GLCMTexture glcm = new GLCMTexture(angle, glcmstep);
		ImageProcessor ip = itext.getProcessor();
		for ( int i = 0; i < (imp.getWidth()-wsize); i+= step )
		{
			for ( int j = 0; j < (imp.getHeight()-wsize); j+= step )
			{
				Roi rect = new Roi( i, j, wsize, wsize );
				rect.setImage(imp);
				imp.setRoi(rect);
				double res = glcm.calcTexture(imp.getProcessor(), rect, texture);
				int x = (int) (i+wsize/2);	
				int y = (int) (j+wsize/2);	
				Roi wind = new Roi( x-step/2, y-step/2, step, step );
				ip.setRoi(wind);
				ip.set(res);
			}
		itext.updateAndDraw();
		IJ.resetMinAndMax(itext);
		itext.show();
		}
	}


	public void run(String arg)
	{
		imp = IJ.getImage();
		getParameters();
		initCalibration();
		itext = IJ.createImage(""+texture, "32-bit black", imp.getWidth(), imp.getHeight(), 1);
		getTexture();

	}

}
