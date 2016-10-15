/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive.plot;

import gov.usgs.plot.data.SliceWave;
import gov.usgs.plot.data.Spectrogram;
import gov.usgs.plot.render.BasicFrameRenderer;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.plot.render.wave.MinuteMarkingWaveRenderer;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.plot.render.wave.SpectrogramRenderer;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Create a combined wave/spectrogram plot for a single channel.
 * 
 * @author Tom Parker
 * 
 */
public abstract class ChannelPlotter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelPlotter.class);
  
  /** Font used to indicate no data available. */
  public static final Color NO_DATA_TEXT_COLOR = new Color(160, 41, 41);

  /** The ratio of a waveform plot to its spectrogram plot. */
  public static final double WAVE_RATIO = .25;

  private static final double DEFAULT_OVERLAP = 0.859375;
  private static final boolean DEFAULT_LOG_POWER = true;
  private static final double DEFAULT_MIN_FREQ = 0;
  private static final double DEFAULT_MAX_FREQ = 10;
  private static final int DEFAULT_NFFT = 0;
  private static final int DEFAULT_BIN_SIZE = 256;
  private static final int DEFAULT_MAX_POWER = 120;
  private static final int DEFAULT_MIN_POWER = 30;

  /** my wave data. */
  protected SliceWave wave;

  /** Font to use for no data message. */
  protected Font noDataFont;

  /** Channel position in plot. */
  protected int index;

  /** Dimension of channel plot. */
  protected Dimension plotDimension;

  /** my config stanza. */
  protected ConfigFile config;

  /** my wave renderer. */
  private final SliceWaveRenderer waveRenderer;

  /** my spectrogram renderer. */
  protected final SpectrogramRenderer spectrogramRenderer;

  /** my frame renderer. */
  private final BasicFrameRenderer plotFrame;

  /** height of the wave panel. */
  protected final int waveHeight;

  /** spectrogram min power. */
  private final int minPower;

  /** spectrogram max power. */
  private final int maxPower;

  /** my name. */
  protected final String name;

  /** plot end time in ms. */
  protected long plotEndMs;

  /** make any type-specific modifications to the SpectrogramRenderer. */
  protected abstract void tweakSpectrogramRenderer(SpectrogramRenderer spectrogramRenderer);

  /** make any type-specific modifications to the SliceWaveRenderer. */
  protected abstract void tweakWaveRenderer(SliceWaveRenderer waveRenderer);

  /** make any type-specific modifications to the no-data Renderer. */
  protected abstract void tweakNoDataRenderer(TextRenderer textRenderer);

  /**
   * Class constructor.
   * 
   * @param name My name
   * 
   * @param index My position on the subnet plot
   * 
   * @param plotDimension The dimension of the plot
   * 
   * @param config My configuration stanza
   */
  public ChannelPlotter(String name, int index, Dimension plotDimension, ConfigFile config) {
    this.name = name;
    this.index = index;
    this.plotDimension = plotDimension;
    this.config = config;
    waveHeight = (int) (plotDimension.height * WAVE_RATIO);

    minPower = config.getInt("minPower", DEFAULT_MIN_POWER);
    maxPower = config.getInt("maxPower", DEFAULT_MAX_POWER);

    waveRenderer = createWaveRenderer();
    spectrogramRenderer = createSpectrogramRenderer(config);

    plotFrame = new BasicFrameRenderer();
    plotFrame.addRenderer(waveRenderer);
    plotFrame.addRenderer(spectrogramRenderer);

  }

  /**
   * Create a SpectrogramRendere and apply my settings.
   * 
   * @param config my config stanza
   * 
   * @return my SpectrogramRenderer
   * 
   */
  protected SpectrogramRenderer createSpectrogramRenderer(ConfigFile config) {

    SpectrogramRenderer sr = new SpectrogramRenderer();

    // y-axis labels will sometimes not be displayed if x-axis tick marks
    // are not displayed. Note sure why.
    sr.yTickMarks = false;
    sr.yTickValues = false;
    sr.xTickMarks = false;
    sr.xTickValues = false;
    sr.xUnits = false;
    sr.xLabel = false;

    sr.setOverlap(config.getDouble("overlap", DEFAULT_OVERLAP));
    sr.setLogPower(config.getBoolean("logPower", DEFAULT_LOG_POWER));
    sr.setMinFreq(config.getDouble("minFreq", DEFAULT_MIN_FREQ));
    sr.setMaxFreq(config.getDouble("maxFreq", DEFAULT_MAX_FREQ));
    sr.setNfft(config.getInt("nfft", DEFAULT_NFFT));
    sr.setBinSize(config.getInt("binSize", DEFAULT_BIN_SIZE));
    sr.setMinPower(minPower);
    sr.setMaxPower(maxPower);
    sr.setTimeZone("UTC");

    tweakSpectrogramRenderer(sr);

    return sr;
  }

  /**
   * Create a WaveRenderer and apply my settings.
   * 
   * @return my SliceWaveRenderer
   */
  protected SliceWaveRenderer createWaveRenderer() {

    SliceWaveRenderer wr = new MinuteMarkingWaveRenderer();

    wr.xTickMarks = false;
    wr.xTickValues = false;
    wr.xUnits = false;
    wr.xLabel = false;
    wr.yTickMarks = false;
    wr.yTickValues = false;
    wr.setColor(Color.BLACK);
    tweakWaveRenderer(wr);

    return wr;
  }

  /**
   * wave mutator method.
   * 
   * @param wave wave to plot
   */
  public void setWave(SliceWave wave) {
    this.wave = wave;
    waveRenderer.setWave(wave);
    spectrogramRenderer.setWave(wave);
    if (wave != null) {
      double plotStart = wave.getStartTime();
      double plotEnd = wave.getStartTime() + SubnetPlotter.DURATION_S;

      waveRenderer.setMinY(wave.min());
      waveRenderer.setMaxY(wave.max());
      waveRenderer.setWave(wave);
      waveRenderer.setViewTimes(plotStart, plotEnd, "UTC");
      waveRenderer.update();

      spectrogramRenderer.setWave(wave);
      spectrogramRenderer.setViewStartTime(plotStart);
      spectrogramRenderer.setViewEndTime(plotEnd);
      spectrogramRenderer.createDefaultFrameDecorator();
      spectrogramRenderer.update();

      plotEndMs = J2kSec.asEpoch(plotEnd);
    }
  }

  /**
   * Produce the plot.
   * 
   * @return frame renderer containing plot or error message
   */
  public BasicFrameRenderer plot() {

    if (wave != null && wave.samples() > 0) {
      return plotFrame;
    } else {
      return noDataRenderer();
    }
  }

  /**
   * Produce a graphical error message indicating that no data is available.
   * 
   * @return A graphical error message
   */
  private BasicFrameRenderer noDataRenderer() {
    int top = index * plotDimension.height;
    TextRenderer tr = new TextRenderer(plotDimension.width / 2, top + plotDimension.height / 2,
        name + " - no data");

    tr.horizJustification = TextRenderer.CENTER;
    tr.vertJustification = TextRenderer.CENTER;
    tr.color = NO_DATA_TEXT_COLOR;
    tr.font = noDataFont;
    tweakNoDataRenderer(tr);

    BasicFrameRenderer fr = new BasicFrameRenderer();
    fr.addRenderer(tr);
    return fr;
  }

  /**
   * accessor method for plotEndMs.
   * 
   * @return plotEndMs
   */
  public long getPlotEndMs() {
    return plotEndMs;
  }

  /**
   * Return data as a CSV string.
   * 
   * @param timestampFormat format to use for the timestamp
   * @return A string CSV representation of the data
   */
  public String getCsv(String timestampFormat) {
    Spectrogram spectrogram = spectrogramRenderer.getSpectrogram();
    if (spectrogram == null) {
      return null;
    }

    double[][] buffer = spectrogram.getSpectraAmplitude();

    long time = plotEndMs - (SubnetPlotter.DURATION_S * 1000);
    long incr = (plotEndMs - time) / buffer.length;

    StringBuffer sb = new StringBuffer();
    for (double[] row : buffer) {
      sb.append(Time.format(timestampFormat, time) + ",");
      for (double col : row) {
        sb.append((int) col + ",");
      }

      sb.deleteCharAt(sb.length() - 1);
      sb.append("\n");
      time += incr;
    }

    return sb.toString();
  }
}
