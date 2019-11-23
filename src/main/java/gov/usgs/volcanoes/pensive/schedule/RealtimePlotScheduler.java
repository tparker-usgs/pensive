/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive.schedule;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

/**
 * A scheduler to queue plots for the most recent time slot.
 * 
 * @author Tom Parker
 *
 */
public class RealtimePlotScheduler extends AbstractPlotScheduler {

  /**
   * Class constructor.
   * 
   * @param name scheduler name
   * @param config scheduler config
   */
  public RealtimePlotScheduler(final String name, final ConfigFile config) {
    super(name, config);
  }

  /**
   * Schedule the next plot for each subnet.
   */
  @Override
  protected void schedulePlots() {
    for (final SubnetPlotter subnet : subnets) {
      try {
        LOGGER.info("Scheduling subnet " + subnet.subnetName);
        plotJobs.put(new PlotJob(subnet));
        if (subnet.printPrevious) {
          LOGGER.info("Scheduling previous plot for subnet " + subnet.subnetName);
          plotJobs.put(new PlotJob(subnet, PlotJob.DO_PRINT_PREVIOUS));        
        }
      } catch (final InterruptedException e) {
        LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
      }
    }
  }

}
