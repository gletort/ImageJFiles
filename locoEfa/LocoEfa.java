
/** 
 * \brief Plugin to calculte LOCO-EFA (Sanchez-Corrales et al 2018) shape description.
 *
 * \details For each z-slice, when there are several Rois associated to that slice, select the "best" one (e.g. largest area)
 *
 * \author G. Letort, College de France
 * \date created on 2019/02/22
 * */

package loco;
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

public class LocoEfa implements PlugIn
{
	
	public double distancePointToPol( FloatPolygon fp, double x, double y)
	{
		double dist = 10000000;
		for ( int i = 0; i < fp.npoints; i++ )
		{
			double d = Math.sqrt( (fp.xpoints[i]-x)*(fp.xpoints[i]-x) + (fp.ypoints[i]-y)*(fp.ypoints[i]-y) );
			if ( d < dist )
				dist = d;
		}
		return dist;
	}

	public double distanceBetweenShapes( Roi shape, Roi efares )
	{
		FloatPolygon sha = shape.getInterpolatedPolygon(1, true);
		FloatPolygon efa = efares.getInterpolatedPolygon(1, true);

		double dist = 0;
		for ( int i = 0; i < sha.npoints; i++ )
		{
			dist += distancePointToPol( efa, sha.xpoints[i], sha.ypoints[i] );
		}
		dist /= sha.npoints;
	return dist;	
	}


	public void run(String arg)
	{

		// Show the reconstructions taking more and more modes
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null )
		{
			IJ.error("Must have a Roi to analyze");
		}
		Roi shape = rm.getRoi(0);

		EFALocoCoef efa = new EFALocoCoef( shape.getFloatPolygon(), 100);
		efa.calcEFACoefficients();
		efa.calcLocoCoefficients();
		double rad = shape.getFeretsDiameter()/2.0;
		double[] cent = shape.getContourCentroid();
		Roi ref = new OvalRoi(cent[0]-rad, cent[1]-rad, 2*rad, 2*rad); 
		
		double dref = distanceBetweenShapes( shape, ref );
		double cumuldist = 0;
		for (int j = 2; j < 50; j+=1)
		{
			Roi res = efa.reconstruct(j);
			rm.addRoi(res);
			double dist = distanceBetweenShapes( shape, res );
			IJ.log("Relative distance mode "+j+": "+dist/dref);
			cumuldist += dist/dref;
		}
		IJ.log("Cumulated distance  "+cumuldist);
		IJ.log("Entropy " + (efa.entropy(50)));
		IJ.log("Max Ln mode: " + (efa.maxContribPos()));

	}
}
