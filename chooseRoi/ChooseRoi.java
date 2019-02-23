package chooser;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.measure.*;
import java.io.*;
import java.util.*;
import java.awt.*;

public class ChooseRoi implements PlugIn 
{
	ImagePlus imp;

	/** \brief Select only one Roi by z-slice
	 *
	 * @param which Criteria to choose a winner Roi to keep by z (e.g. biggest area)*/
	public void keepRois( int which )
	{
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() == 0 )
		{
			IJ.error("No Rois allRois in Manager");
			return;
		}

		rm.runCommand( imp, "Sort" );
		Roi[] allRois = rm.getRoisAsArray();
		int curz;
		int	wini = 0;
		int doingz = -1;
		double refarea;
		switch ( which )
		{
			case 0:
				refarea = 0;
				break;
			case 1:
				refarea = imp.getWidth()*imp.getHeight() + 2;
				break;
			default:
				refarea = 0;
				break;
		}
		double marea = refarea;
		Vector tokeep = new Vector();
		tokeep.clear();
		for ( int i = 0; i < allRois.length; i++ )
		{
			Roi curroi = allRois[i];
			curz = curroi.getPosition();
			if ( doingz == -1 ) doingz = curz;
			if ( curz > doingz )
			{
				tokeep.add( wini );
				marea = refarea;
				doingz = curz;
			}
			rm.select(i);
			double tmparea = imp.getStatistics().area; 
			switch ( which )
			{
				case 0:
					// biggest area wins
					if ( tmparea > marea )
					{
						wini = i;
						marea = tmparea;
					}
					break;
				case 1:
					// smallest area wins
					if ( tmparea < marea )
					{
						wini = i;
						marea = tmparea;
					}
					break;
				default:
					break;
			}
		}
		// add last z
		tokeep.add( wini );
		marea = 0;

		int ndel = allRois.length-tokeep.size();
		if ( ndel > 0 )
		{
			int[] dels = new int[ndel];
			int filled = 0;
			for ( int j = 0; j < allRois.length; j++ )
			{
				if ( !tokeep.contains(j) )
				{
					dels[filled] = j;
					filled = filled + 1;
				}
			}
			rm.setSelectedIndexes(dels);
			rm.runCommand("Delete");
		}
	}	

	/** \brief Select one roi by z-slice (biggest area, smallest area)*/
	public void run(String arg) 
	{
		IJ.run("Select None");
		imp = IJ.getImage();

		switch ( arg.toLowerCase() )
		{
			case "biggest": 
				keepRois(0);
				break;
			case "smallest": 
				keepRois(1);
				break;
			default:
				keepRois(0);
				break;
		}
	}		

}
