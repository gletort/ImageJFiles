package ellip;
import java.util.*;
import java.util.Locale;
import java.text.*;


public class Ellipsoid
{

	private Vector pts;  // set of points
	private double scalez = 4; // value of z slice in µm	
	private double scalex = 0.09; // value of scaling
	private double scaley = 0.09; // value of scaling
	public double volume; // calcualted volume
	private Point3D centroid; // centroid of the set of points
	private double[][] mat; //ellipsoid fit 
	private double a,b,c; //ellipsoid radii

	public Ellipsoid() 
	{ 
		pts = new Vector();  
		centroid = new Point3D() ; 
	}

	public void setScale( double sx, double sy, double sz )
	{
		scalex = sx;
		scaley = sy;
		scalez = sz;
	}

	public void addPointScaled( double x, double y, int z )
	{
		Point3D pt = new Point3D( x, y, z*scalez );
		updateCentroid( pt );
		pts.add( pt );
	}

	public void addPoint( double x, double y, int z )
	{
		Point3D pt = new Point3D( scalex*x, scaley*y, z*scalez );
		updateCentroid( pt );
		pts.add( pt );
	}

	public void setVolume( double vol )
	{
		volume = vol * scalez;
	}

	public void updateCentroid(Point3D pt)
	{
		int n = (pts.size());
		centroid.multiply( n );
		centroid.addPoint(pt);
		centroid.multiply( 1.0/(n+1) ); 
	}

	public Point3D centroidInPixels()
	{
		Point3D scalecent = new Point3D();
		scalecent.scalePoint( centroid, 1.0/scalex, 1.0/scaley, 1.0/scalez );
		return scalecent;
	}

	public Point3D getCentroid()
	{
		return centroid;
	}

	public double[][] getMatrix() 
	{ 
		return mat;
	}

	public double[] getLengths() 
	{
		double[] res = new double[3];
		res[0] = a;
		res[1] = b;
		res[2] = c;
		return res;
	}

	public String getEllipsoid()
	{
		if ( pts.size() <= 1 ) return "Failed";
		// Calculate selection moments
		double s200 = 0;
		double s110 = 0;
		double s101 = 0;
		double s020 = 0;
		double s011 = 0;
		double s002 = 0;

		double i, j, k;
		double bx = centroid.x();
		double by = centroid.y();
		double bz = centroid.z();

		Point3D pt;
		for ( int ind = 0; ind < pts.size(); ind++ ) 
		{
			pt = (Point3D) pts.get(ind);
			i = pt.x();
			j = pt.y();
			k = pt.z();
			s200 += (j - by) * (j - by) + (k - bz) * (k - bz);
			s020 += (i - bx) * (i - bx) + (k - bz) * (k - bz);
			s002 += (i - bx) * (i - bx) + (j - by) * (j - by);
			s110 += (i - bx) * (j - by);
			s101 += (i - bx) * (k - bz);
			s011 += (j - by) * (k - bz);
		}
		double rescale = 1.0/pts.size();
		s200 *= rescale;
		s020 *= rescale;
		s002 *= rescale;
		s110 *= rescale;
		s101 *= rescale;
		s011 *= rescale;

		// calculate eigenvalues and vectors of selection
		mat = new double[3][3];
		mat[0][0] = s200;
		mat[0][1] = -s110;
		mat[0][2] = -s101;
		mat[1][0] = -s110;
		mat[1][1] = s020;
		mat[1][2] = -s011;
		mat[2][0] = -s101;
		mat[2][1] = -s011;
		mat[2][2] = s002;

		EigenvalueDecomposition eigen = new EigenvalueDecomposition(mat);
		double[] eigv = eigen.getRealEigenvalues();
		a = Math.sqrt( 2.5 * ( eigv[1]+eigv[2]-eigv[0]) );
		b = Math.sqrt( 2.5 * ( eigv[0]+eigv[2]-eigv[1]) );
		c = Math.sqrt( 2.5 * ( eigv[0]+eigv[1]-eigv[2]) );
		
		double volMaj = 4.0/3.0*Math.PI*a*a*a;
		double vol = 4.0/3.0*Math.PI*a*b*c;

		mat = eigen.getV();

		String form = "#.###";
		Locale currentLocale = Locale.getDefault();
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(currentLocale);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator('.'); 
		DecimalFormat df = new DecimalFormat(form, otherSymbols);
		String res = ""+df.format(a)+"\t"+df.format(b)+"\t"+df.format(c)+"\t"+df.format(vol)+"\t"+df.format((b+c)/(2*a)) ;
		return res;
	}

	public boolean isEllipsoidContour(double i, double j, double k)
	{
			
		double x = (i*scalex - centroid.x());
		if ( Math.abs(x) > a*2 ) return false;
		double y = (j*scaley - centroid.y());
		if ( Math.abs(y) > a*2 ) return false;
		double z = (k*scalez - centroid.z());
		if ( Math.abs(z) > a*2 ) return false;

		double xp = mat[0][0]*x + mat[1][0]*y +mat[2][0]*z;	
		double yp = mat[0][1]*x + mat[1][1]*y +mat[2][1]*z;	
		double zp = mat[0][2]*x + mat[1][2]*y +mat[2][2]*z;
		double res = xp*xp / (a*a) + yp*yp/(b*b) + zp*zp/(c*c);

		return ( Math.abs(1-res) <= 0.01 );

	}

	public boolean isBounding(double i, double j, double k, double[] max, double[] min)
	{
			
		double x = (i*scalex - centroid.x());
		double y = (j*scaley - centroid.y());
		double z = (k*scalez - centroid.z());
		
		double xp = mat[0][0]*x + mat[1][0]*y +mat[2][0]*z;	
		double yp = mat[0][1]*x + mat[1][1]*y +mat[2][1]*z;	
		double zp = mat[0][2]*x + mat[1][2]*y +mat[2][2]*z;

		if ( Math.abs(xp-max[0]) <= 0.3 && Math.abs(xp)<=2*a) return true;
		if ( Math.abs(xp-min[0]) <= 0.3 && Math.abs(xp)<=2*a) return true;
		if ( Math.abs(yp-max[1]) <= 0.1 && Math.abs(yp)<=2*b) return true;
		if ( Math.abs(yp-min[1]) <= 0.1 && Math.abs(yp)<=2*b) return true;
		if ( Math.abs(zp-min[2]) <= 0.1 && Math.abs(zp)<=2*c) return true;
		if ( Math.abs(zp-max[2]) <= 0.1 && Math.abs(zp)<=2*c) return true;

		return false;
	}
	public double[] coorInEllip( double xi, double yj, double zk )
	{
		double[] coord = {0,0,0};
		if ( pts.size() <= 1 ) return coord;

		double x = (xi*scalex - centroid.x());
		double y = (yj*scaley - centroid.y());
		double z = (zk*scalez - centroid.z());

		coord[0] = mat[0][0]*x + mat[1][0]*y +mat[2][0]*z;	
		coord[1] = mat[0][1]*x + mat[1][1]*y +mat[2][1]*z;	
		coord[2] = mat[0][2]*x + mat[1][2]*y +mat[2][2]*z;

		return coord;
	}

}
