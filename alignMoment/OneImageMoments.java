package align;
import ij.*;
import ij.plugin.filter.*;
import ij.process.*;

public class OneImageMoments
{

	private double xc;
	private double yc;
	private double angle;

	ImagePlus imp;

	public OneImageMoments()
	{
	}

	public OneImageMoments(ImagePlus ip)
	{
		imp = ip;
	}

	public void setImage( ImagePlus ip )
	{
		imp = ip;
	}

	public void getMoments(double powing)
	{
		int wid = imp.getWidth();
		int hei = imp.getHeight();
		float pxy;
		ImageProcessor ip = imp.getProcessor();

		/// calculate first moments, centroid
		double m00 = 0;
		double m10 = 0;
		double m01 = 0;
		/// second moments
		double m20 = 0;
		double m02 = 0;
		double m11 = 0;
		
		ImageStatistics stats = ip.getStatistics();
		float max = (float) stats.max;
		for ( int x = 0; x < wid; x++ )
		{
			for ( int y = 0; y < hei; y++ )
			{
				pxy = ip.getPixelValue( x, y )/max;
				// for non binary, to reduce noise weight 
				pxy = (float) Math.pow(pxy, powing);
				m00 += pxy;
				m10 += pxy * x;
				m01 += pxy * y;

				m20 += pxy * x * x;
				m02 += pxy * y * y;
				m11 += pxy * x * y;
			}
		}

		double xbar = m10/m00;
		double ybar = m01/m00;
		//	System.out.println(m00+" "+xbar+" "+ybar);

		double mup20 = m20/m00 - xbar*xbar;
		double mup02 = m02/m00 - ybar*ybar;
		double mup11 = m11/m00 - xbar*ybar;

		//	System.out.println(mup11+" "+(mup20-mup02));
		//double theta = 0.5 * Math.atan( (2*mup11)/(mup20-mup02) );
		double theta = Math.toDegrees(0.5*Math.atan2(2.0*mup11, mup20-mup02));

		xc = xbar;
		yc = ybar;
		angle = theta;
		//if ( mup20 < mup02 ) angle += Math.PI/2;
		//angle = angle * 180 / Math.PI;

		//angle = angle - 90;
		//	System.out.println( angle );
	}

	public void printValues(int z)
	{
		IJ.log( z+" \t "+xc+" \t "+yc+" \t "+angle );
	}

	public void translate()
	{
		double xt = imp.getWidth()/2 - xc;
		double yt = imp.getHeight()/2 - yc;
		ImageProcessor ip = imp.getProcessor();
		ip.setInterpolationMethod(ImageProcessor.BILINEAR);
		ip.translate(xt, yt);
		imp.setProcessor(ip);
		//IJ.run(imp, "Translate...", "x="+xt+" y="+yt+" interpolation=Bilinear slice");
	}

	public void rotate()
	{
		ImageProcessor ip = imp.getProcessor();
		ip.setBackgroundValue(0);
		ip.setInterpolationMethod(ImageProcessor.BILINEAR);
		ip.rotate(-angle);
		imp.setProcessor(ip);
		//IJ.run(imp, "Rotate... ", "angle="+(-angle)+" grid=1 interpolation=Bilinear slice");
	}
}
