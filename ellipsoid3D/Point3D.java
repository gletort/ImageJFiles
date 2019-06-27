package ellip;

public class Point3D
{
	private double[] coordonnees; // can be in pixel or µm

	public Point3D() 
	{
		coordonnees = new double[3];
		for ( int i = 0; i < 3; i ++ )
		{
			coordonnees[i] = 0;
		}
	}

	public Point3D( double x, double y, double z )
	{
		coordonnees = new double[3];
		coordonnees[0] = x;
		coordonnees[1] = y;
		coordonnees[2] = z;
	}

	public void setCoordinates( double x, double y, double z )
	{
		coordonnees[0] = x;
		coordonnees[1] = y;
		coordonnees[2] = z;
	}

	public double x() { return coordonnees[0]; }
	public double y() { return coordonnees[1]; }
	public double z() { return coordonnees[2]; }
	public double get(int i) { return coordonnees[i]; }

	public void addPoint( Point3D add )
	{
		coordonnees[0] = coordonnees[0] + add.x();
		coordonnees[1] = coordonnees[1] + add.y();
		coordonnees[2] = coordonnees[2] + add.z();
	}
	
	public void addValues( double x, double y, double z )
	{
		coordonnees[0] = coordonnees[0] + x;
		coordonnees[1] = coordonnees[1] + y;
		coordonnees[2] = coordonnees[2] + z;
	}

	public void divide( int n )
	{
		if ( n != 0 )
		{
			for ( int i = 0; i < 3; i++ )
				coordonnees[i] = coordonnees[i] / n;
		}
	}
	
	public void multiply( double fact )
	{
		for ( int i = 0; i < 3; i++ )
			coordonnees[i] = coordonnees[i] * fact;
	}

	public void scalePoint( Point3D pt, double sx, double sy, double sz )
	{
		coordonnees[0] = pt.coordonnees[0] * sx;
		coordonnees[1] = pt.coordonnees[1] * sy;
		coordonnees[2] = pt.coordonnees[2] * sz;
	}

	public void scale( double sx, double sy, double sz )
	{
		coordonnees[0] = coordonnees[0] * sx;
		coordonnees[1] = coordonnees[1] * sy;
		coordonnees[2] = coordonnees[2] * sz;
	}

	public double distance( Point3D p )
	{
		return Math.sqrt( (x()-p.x())*(x()-p.x()) + (y()-p.y())*(y()-p.y()) + (z()-p.z())*(z()-p.z()) ); 
	}
}
