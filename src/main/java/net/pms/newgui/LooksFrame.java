/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.newgui;

//import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.io.WindowsNamedPipe;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.update.AutoUpdateDialog;
import net.pms.update.AutoUpdater;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LooksFrame extends JFrame implements IFrame, Observer {
	private static final Logger LOGGER = LoggerFactory.getLogger(LooksFrame.class);

	private final AutoUpdater autoUpdater;
	private final PmsConfiguration configuration;
	public static final String START_SERVICE = "start.service";
	private static final long serialVersionUID = 8723727186288427690L;
	private Dimension storedWindowSize = new Dimension();
	private Dimension storedScreenSize = new Dimension();
	protected static final Dimension STANDARD_SIZE = new Dimension(1000, 750);
	// https://code.google.com/p/ps3mediaserver/issues/detail?id=949
	protected static final Dimension MINIMUM_SIZE = new Dimension(800, 480);
	private Dimension screenSize = getToolkit().getScreenSize();

	/**
	 * List of context sensitive help pages URLs. These URLs should be
	 * relative to the documentation directory and in the same order as the
	 * tabs. The value <code>null</code> means "don't care", activating the
	 * tab will not change the help page.
	 */
	protected static final String[] HELP_PAGES = {
		"index.html",
		null,
		"general_configuration.html",
		null,
		"navigation_share.html",
		"transcoding.html",
		null,
		null
	};

	private NavigationShareTab nt;
	private StatusTab st;
	private TracesTab tt;
	private TranscodingTab tr;
	private GeneralTab gt;
	private HelpTab ht;
	private PluginTab pt;
	private AbstractButton reload;
	private AbstractButton webinterface;
	private JLabel status;
	private static Object lookAndFeelInitializedLock = new Object();
	private static boolean lookAndFeelInitialized = false;
	private ViewLevel viewLevel = ViewLevel.UNKNOWN;

	/**
	 * Class name of Windows L&F provided in Sun JDK.
	 */
	public static final String WINDOWS_LNF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

	/**
	 * Class name of PlasticXP L&F.
	 */
	public static final String PLASTICXP_LNF = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";

	/**
	 * Class name of Metal L&F.
	 */
	public static final String METAL_LNF = "javax.swing.plaf.metal.MetalLookAndFeel";

	public ViewLevel getViewLevel() {
		return viewLevel;
	}

	public void setViewLevel(ViewLevel viewLevel) {
		if (viewLevel != ViewLevel.UNKNOWN){
			this.viewLevel = viewLevel;
			tt.applyViewLevel();
		}
	}

	public TracesTab getTt() {
		return tt;
	}

	public NavigationShareTab getNt() {
		return nt;
	}

	public TranscodingTab getTr() {
		return tr;
	}

	public GeneralTab getGt() {
		return gt;
	}

	public PluginTab getPt() {
		return pt;
	}

	public AbstractButton getReload() {
		return reload;
	}

	public static void initializeLookAndFeel() {
		synchronized (lookAndFeelInitializedLock) {
			if (lookAndFeelInitialized) {
				return;
			}

			if (Platform.isWindows()) {
				try {
					UIManager.setLookAndFeel(WINDOWS_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Windows look and feel: ", e);
				}
			} else if (System.getProperty("nativelook") == null && !Platform.isMac()) {
				try {
					UIManager.setLookAndFeel(PLASTICXP_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Plastic XP look and feel: ", e);
				}
			} else {
				try {
					String systemClassName = UIManager.getSystemLookAndFeelClassName();
					// Workaround for Gnome
					try {
						String gtkLAF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
						Class.forName(gtkLAF);

						if (systemClassName.equals("javax.swing.plaf.metal.MetalLookAndFeel")) {
							systemClassName = gtkLAF;
						}
					} catch (ClassNotFoundException ce) {
						LOGGER.error("Error loading GTK look and feel: ", ce);
					}

					LOGGER.trace("Choosing Java look and feel: " + systemClassName);
					UIManager.setLookAndFeel(systemClassName);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
					try {
						UIManager.setLookAndFeel(PLASTICXP_LNF);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
						LOGGER.error("Error while setting Plastic XP look and feel: ", e);
					}
					LOGGER.error("Error while setting native look and feel: ", e1);
				}
			}

			if (isParticularLaFSet(UIManager.getLookAndFeel(), PLASTICXP_LNF)) {
				PlasticLookAndFeel.setPlasticTheme(PlasticLookAndFeel.createMyDefaultTheme());
				PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
				PlasticLookAndFeel.setHighContrastFocusColorsEnabled(false);
			} else if (isParticularLaFSet(UIManager.getLookAndFeel(), METAL_LNF)) {
				MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
			}

			// Work around caching in MetalRadioButtonUI
			JRadioButton radio = new JRadioButton();
			radio.getUI().uninstallUI(radio);
			JCheckBox checkBox = new JCheckBox();
			checkBox.getUI().uninstallUI(checkBox);

			// Workaround for JDK-8179014: JFileChooser with Windows look and feel crashes on win 10
			// https://bugs.openjdk.java.net/browse/JDK-8179014
			if (isParticularLaFSet(UIManager.getLookAndFeel(), WINDOWS_LNF)) {
				UIManager.put("FileChooser.useSystemExtensionHiding", false);
			}

			lookAndFeelInitialized = true;
		}
	}

	/**
	 * Safely checks whether a particular look and feel class is set.
	 *
	 * @param lnf
	 * @param lookAndFeelClassPath
	 * @return whether the incoming look and feel class is set
	 */
	private static boolean isParticularLaFSet(LookAndFeel lnf, String lookAndFeelClassPath) {
		// as of Java 10, com.sun.java.swing.plaf.windows.WindowsLookAndFeel
		// is no longer available on macOS
		// thus "instanceof WindowsLookAndFeel" directives will result
		// in a NoClassDefFoundError during runtime
		if (lnf == null) {
			return false;
		} else {
			try {
				Class c = Class.forName(lookAndFeelClassPath);
				return c.isInstance(lnf);
			} catch (ClassNotFoundException cnfe) {
				// if it is not possible to load the Windows LnF class, the
				// given lnf instance cannot be an instance of the Windows
				// LnF class
				return false;
			}
		}
	}

	/**
	 * Constructs a <code>DemoFrame</code>, configures the UI,
	 * and builds the content.
	 */
	public LooksFrame(AutoUpdater autoUpdater, PmsConfiguration configuration) {
		this.autoUpdater = autoUpdater;
		this.configuration = configuration;
		assert this.configuration != null;
		Options.setDefaultIconSize(new Dimension(18, 18));
		Options.setUseNarrowButtons(true);

		// Set view level, can be omitted if ViewLevel is implemented in configuration
		// by setting the view level as variable initialization
		if (configuration.isHideAdvancedOptions()) {
			viewLevel = ViewLevel.NORMAL;
		} else {
			viewLevel = ViewLevel.ADVANCED;
		}

		// Global options
		Options.setTabIconsEnabled(true);
		UIManager.put(Options.POPUP_DROP_SHADOW_ENABLED_KEY, null);

		// Swing Settings
		initializeLookAndFeel();

		// wait till the look and feel has been initialized before (possibly) displaying the update notification dialog
		if (autoUpdater != null) {
			autoUpdater.addObserver(this);
			autoUpdater.pollServer();
		}

		// http://propedit.sourceforge.jp/propertieseditor.jnlp
		Font sf = null;

		// Set an unicode font for testing exotic languages (Japanese)
		final String language = configuration.getLanguageTag();

		if (language != null && (language.equals("ja") || language.startsWith("zh") || language.equals("ko"))) {
			sf = new Font("SansSerif", Font.PLAIN, 12);
		}

		if (sf != null) {
			UIManager.put("Button.font", sf);
			UIManager.put("ToggleButton.font", sf);
			UIManager.put("RadioButton.font", sf);
			UIManager.put("CheckBox.font", sf);
			UIManager.put("ColorChooser.font", sf);
			UIManager.put("ToggleButton.font", sf);
			UIManager.put("ComboBox.font", sf);
			UIManager.put("ComboBoxItem.font", sf);
			UIManager.put("InternalFrame.titleFont", sf);
			UIManager.put("Label.font", sf);
			UIManager.put("List.font", sf);
			UIManager.put("MenuBar.font", sf);
			UIManager.put("Menu.font", sf);
			UIManager.put("MenuItem.font", sf);
			UIManager.put("RadioButtonMenuItem.font", sf);
			UIManager.put("CheckBoxMenuItem.font", sf);
			UIManager.put("PopupMenu.font", sf);
			UIManager.put("OptionPane.font", sf);
			UIManager.put("Panel.font", sf);
			UIManager.put("ProgressBar.font", sf);
			UIManager.put("ScrollPane.font", sf);
			UIManager.put("Viewport", sf);
			UIManager.put("TabbedPane.font", sf);
			UIManager.put("TableHeader.font", sf);
			UIManager.put("TextField.font", sf);
			UIManager.put("PasswordFiled.font", sf);
			UIManager.put("TextArea.font", sf);
			UIManager.put("TextPane.font", sf);
			UIManager.put("EditorPane.font", sf);
			UIManager.put("TitledBorder.font", sf);
			UIManager.put("ToolBar.font", sf);
			UIManager.put("ToolTip.font", sf);
			UIManager.put("Tree.font", sf);
			UIManager.put("Spinner.font", sf);
		}
		
		// *** Shared Fonts
		Integer twelve = Integer.valueOf(12);
		Integer fontPlain = Integer.valueOf(Font.PLAIN);
		Integer fontBold = Integer.valueOf(Font.BOLD);

		LazyValue dialogPlain12 = t -> new FontUIResource(Font.DIALOG, fontPlain, twelve);
		LazyValue sansSerifPlain12 =  t -> new FontUIResource(Font.SANS_SERIF, fontPlain, twelve);
		LazyValue monospacedPlain12 = t -> new FontUIResource(Font.MONOSPACED, fontPlain, twelve);
		LazyValue dialogBold12 = t -> new FontUIResource(Font.DIALOG, fontBold, twelve);

		Object MenuFont = dialogPlain12;
		Object FixedControlFont = monospacedPlain12;
		Object ControlFont = dialogPlain12;
		Object MessageFont = dialogPlain12;
		Object WindowFont = dialogBold12;
		Object ToolTipFont = sansSerifPlain12;
		Object IconFont = ControlFont;

		UIManager.put("Button.font", ControlFont);
		UIManager.put("CheckBox.font", ControlFont);
		UIManager.put("CheckBoxMenuItem.font", MenuFont);
		UIManager.put("ComboBox.font", ControlFont);
		UIManager.put("EditorPane.font", ControlFont);
		UIManager.put("FileChooser.listFont", IconFont);
		UIManager.put("FormattedTextField.font", ControlFont);
		UIManager.put("InternalFrame.titleFont", WindowFont);
		UIManager.put("Label.font", ControlFont);
		UIManager.put("List.font", ControlFont);
		UIManager.put("PopupMenu.font", MenuFont);
		UIManager.put("Menu.font", MenuFont);
		UIManager.put("MenuBar.font", MenuFont);
		UIManager.put("MenuItem.font", MenuFont);
		UIManager.put("MenuItem.acceleratorFont", MenuFont);
		UIManager.put("RadioButton.font", ControlFont);
		UIManager.put("RadioButtonMenuItem.font", MenuFont);
		UIManager.put("OptionPane.font", MessageFont);
		UIManager.put("OptionPane.messageFont", MessageFont);
		UIManager.put("OptionPane.buttonFont", MessageFont);
		UIManager.put("Panel.font", ControlFont);
		UIManager.put("PasswordField.font", ControlFont);
		UIManager.put("ProgressBar.font", ControlFont);
		UIManager.put("ScrollPane.font", ControlFont);
		UIManager.put("Slider.font", ControlFont);
		UIManager.put("Spinner.font", ControlFont);
		UIManager.put("TabbedPane.font", ControlFont);
		UIManager.put("Table.font", ControlFont);
		UIManager.put("TableHeader.font", ControlFont);
		UIManager.put("TextArea.font", FixedControlFont);
		UIManager.put("TextField.font", ControlFont);
		UIManager.put("TextPane.font", ControlFont);
		UIManager.put("TitledBorder.font", ControlFont);
		UIManager.put("ToggleButton.font", ControlFont);
		UIManager.put("ToolBar.font", MenuFont);
		UIManager.put("ToolTip.font", ToolTipFont);
		UIManager.put("Tree.font", ControlFont);
		UIManager.put("Viewport.font", ControlFont);

		setTitle("Test");
		setIconImage(readImageIcon("icon-32.png").getImage());

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JComponent jp = buildContent();
		String showScrollbars = System.getProperty("scrollbars", "").toLowerCase();

		/**
		 * Handle scrollbars:
		 *
		 * 1) forced scrollbars (-Dscrollbars=true): always display them
		 * 2) optional scrollbars (-Dscrollbars=optional): display them as needed
		 * 3) otherwise (default): don't display them
		 */
		switch (showScrollbars) {
			case "true":
				setContentPane(
					new JScrollPane(
						jp,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
					)
				);
				break;
			case "optional":
				setContentPane(
					new JScrollPane(
						jp,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
					)
				);
				break;
			default:
				setContentPane(jp);
				break;
		}

		String projectName = PropertiesUtil.getProjectProperties().get("project.name");
		String projectVersion = PropertiesUtil.getProjectProperties().get("project.version");
		String title = projectName + " " + projectVersion;

		// If the version contains a "-" (e.g. "1.50.1-SNAPSHOT" or "1.50.1-beta1"), add a warning message
		if (projectVersion.indexOf('-') > -1) {
			title = title + " - " + Messages.getString("LooksFrame.26");
		}

		if (PMS.getTraceMode() == 2) {
			// Forced trace mode
			title = title + "  [" + Messages.getString("TracesTab.10").toUpperCase() + "]";
		}

		setTitle(title);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		if (screenSize.width < MINIMUM_SIZE.width || screenSize.height < MINIMUM_SIZE.height) {
			setMinimumSize(screenSize);
		} else {
			setMinimumSize(MINIMUM_SIZE);
		}

		String ss = configuration.getScreenSize();
		storedScreenSize.height = Integer.parseInt(ss.substring(ss.indexOf('x') + 1));
		storedScreenSize.width = Integer.parseInt(ss.substring(0, ss.indexOf('x')));
		String[] windowGeometryValues = configuration.getWindowGeometry().split(",");
		int posX = Integer.parseInt(windowGeometryValues[0].substring(windowGeometryValues[0].indexOf('=') + 1));
		int posY = Integer.parseInt(windowGeometryValues[1].substring(windowGeometryValues[1].indexOf('=') + 1));
		storedWindowSize.width = Integer.parseInt(windowGeometryValues[2].substring(windowGeometryValues[2].indexOf('=') + 1));
		storedWindowSize.height = Integer.parseInt(windowGeometryValues[3].substring(windowGeometryValues[3].indexOf('=') + 1));
		setSize(storedWindowSize);
		boolean screenChanged = false;
		if (storedScreenSize.width != screenSize.getWidth() || storedScreenSize.height != screenSize.getHeight()) {
			setSize(STANDARD_SIZE);
			screenChanged = true;
		} else if (configuration.getWindowExtendedState() != NORMAL) {
			setExtendedState(configuration.getWindowExtendedState());
		} else if (screenSize.width < storedWindowSize.width || screenSize.height < storedWindowSize.height) {
			setSize(screenSize);
		}

		// Customize the colors used in tooltips
		UIManager.put("ToolTip.background", new ColorUIResource(PMS.getConfiguration().getToolTipBackgroundColor()));
		Border border = BorderFactory.createLineBorder(PMS.getConfiguration().getToolTipBackgroundColor(), 4);
		UIManager.put("ToolTip.border", border);
		UIManager.put("ToolTip.foreground", new ColorUIResource(PMS.getConfiguration().getToolTipForegroundColor()));

		// Display tooltips immediately and for a long time
		ToolTipManager.sharedInstance().setInitialDelay(400);
		ToolTipManager.sharedInstance().setDismissDelay(60000);
		ToolTipManager.sharedInstance().setReshowDelay(400);

		setResizable(true);
		Dimension paneSize = getSize();
		if (posX == -1 && posY == -1 || screenChanged) { // first run of UMS or screen/desktop was changed so set the position to the middle of the screen
			setLocation(
			((screenSize.width > paneSize.width) ? ((screenSize.width - paneSize.width) / 2) : 0),
			((screenSize.height > paneSize.height) ? ((screenSize.height - paneSize.height) / 2) : 0)
			);
		} else {
			setLocation(posX, posY);
		}

		if (!configuration.isMinimized() && System.getProperty(START_SERVICE) == null) {
			setVisible(true);
		}
		PMS.get().getRegistry().addSystemTray(this);
	}

	public static ImageIcon readImageIcon(String filename) {
		URL url = LooksFrame.class.getResource("/resources/images/" + filename);
		return new ImageIcon(url);
	}

	public JComponent buildContent() {
		JPanel panel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		toolBar.add(new JPanel());

		if (PMS.getConfiguration().useWebInterface()) {
			webinterface = createToolBarButton(Messages.getString("LooksFrame.29"), "button-webinterface.png");
			webinterface.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI(PMS.get().getWebInterface().getUrl()));
					} catch (IOException | URISyntaxException e2) {
						LOGGER.trace("Unable to open the given URI: " + PMS.get().getWebInterface().getUrl() + ".");
					}
				}
			});
			webinterface.setToolTipText(Messages.getString("LooksFrame.30"));
			toolBar.add(webinterface);
			toolBar.addSeparator(new Dimension(20, 1));
		}

		reload = createToolBarButton(Messages.getString("LooksFrame.12"), "button-restart.png");
		reload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PMS.get().reset();
			}
		});
		reload.setToolTipText(Messages.getString("LooksFrame.28"));
		toolBar.add(reload);

		toolBar.addSeparator(new Dimension(20, 1));
		AbstractButton quit = createToolBarButton(Messages.getString("LooksFrame.5"), "button-quit.png");
		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});
		toolBar.add(quit);
		if (System.getProperty(START_SERVICE) != null) {
			quit.setEnabled(false);
		}
		toolBar.add(new JPanel());

		// Apply the orientation to the toolbar and all components in it
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		toolBar.applyComponentOrientation(orientation);
		toolBar.setBorder(new EmptyBorder(new Insets(8,0,0,0)));

		panel.add(toolBar, BorderLayout.NORTH);
		panel.add(buildMain(), BorderLayout.CENTER);
		status = new JLabel("");
		status.setBorder(BorderFactory.createEmptyBorder());
		status.setComponentOrientation(orientation);

		// Calling applyComponentOrientation() here would be ideal.
		// Alas it horribly mutilates the layout of several tabs.
		//panel.applyComponentOrientation(orientation);
		panel.add(status, BorderLayout.SOUTH);

		return panel;
	}

	public JComponent buildMain() {
		final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

		tabbedPane.setUI(new CustomTabbedPaneUI());

		st = new StatusTab(configuration);
		tt = new TracesTab(configuration, this);
		gt = new GeneralTab(configuration, this);
		pt = new PluginTab(configuration, this);
		nt = new NavigationShareTab(configuration, this);
		tr = new TranscodingTab(configuration, this);
		ht = new HelpTab();

		tabbedPane.addTab(Messages.getString("LooksFrame.18"), st.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.19"), tt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.20"), gt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.27"), pt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.22"), nt.build());
		if (!configuration.isDisableTranscoding()) {
			tabbedPane.addTab(Messages.getString("LooksFrame.21"), tr.build());
		} else {
			tr.build();
		}
		tabbedPane.addTab(Messages.getString("LooksFrame.24"), new HelpTab().build());
		tabbedPane.addTab(Messages.getString("LooksFrame.25"), new AboutTab().build());

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int selectedIndex = tabbedPane.getSelectedIndex();

				if (HELP_PAGES[selectedIndex] != null) {
					PMS.setHelpPage(HELP_PAGES[selectedIndex]);

					// Update the contents of the help tab itself
					ht.updateContents();
				}
			}
		});

		tabbedPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		/*
		 * Set the orientation of the tabbedPane.
		 * Note: Do not use applyComponentOrientation() here because it
		 * messes with the layout of several tabs.
		 */
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		tabbedPane.setComponentOrientation(orientation);

		return tabbedPane;
	}

	protected AbstractButton createToolBarButton(String text, String iconName) {
		CustomJButton button = new CustomJButton(text, readImageIcon(iconName));
		button.setFocusable(false);
		button.setBorderPainted(false);
		return button;
	}

	protected AbstractButton createToolBarButton(String text, String iconName, String toolTipText) {
		CustomJButton button = new CustomJButton(text, readImageIcon(iconName));
		button.setToolTipText(toolTipText);
		button.setFocusable(false);
		button.setBorderPainted(false);
		return button;
	}

	public void quit() {
		WindowsNamedPipe.setLoop(false);
		String windowGeometry = getBounds().toString();
		try {
			if (getExtendedState() != NORMAL) {
				configuration.setWindowExtendedState(getExtendedState());
			} else {
				configuration.setWindowExtendedState(NORMAL);
				configuration.setWindowGeometry(windowGeometry.substring(windowGeometry.indexOf('[') + 1, windowGeometry.indexOf(']')));
			}
			configuration.setScreenSize((int) screenSize.getWidth() + "x" + (int) screenSize.getHeight());
		} catch (Exception e) {
			LOGGER.warn("Failed to save window geometry and size: {}", e.getMessage());
			LOGGER.debug("", e);
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted during shutdown: {}", e);
		}

		System.exit(0);
	}

	@Override
	public void append(final String msg) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				tt.append(msg);
			}
		});
	}

	@Override
	public void setReadValue(long v, String msg) {
		st.setReadValue(v, msg);
	}

	@Override
	public void setStatusCode(int code, String msg, String icon) {
		st.getJl().setText(msg);

		try {
			st.getImagePanel().set(ImageIO.read(LooksFrame.class.getResourceAsStream("/resources/images/" + icon)));
		} catch (IOException e) {
			LOGGER.error(null, e);
		}
	}

	@Override
	public void updateBuffer() {
		st.updateCurrentBitrate();
	}

	/**
	 * This method is being called when a configuration change requiring
	 * a restart of the HTTP server has been done by the user. It should notify the user
	 * to restart the server.<br>
	 * Currently the icon as well as the tool tip text of the restart button is being
	 * changed.<br>
	 * The actions requiring a server restart are defined by {@link PmsConfiguration#NEED_RELOAD_FLAGS}
	 *
	 * @param bool true if the server has to be restarted, false otherwise
	 */
	@Override
	public void setReloadable(boolean bool) {
		if (bool) {
			reload.setIcon(readImageIcon("button-restart-required.png"));
			reload.setToolTipText(Messages.getString("LooksFrame.13"));
		} else {
			reload.setIcon(readImageIcon("button-restart.png"));
			reload.setToolTipText(Messages.getString("LooksFrame.28"));
		}
	}

	@Override
	public void addEngines() {
		tr.addEngines();
	}

	// Fired on AutoUpdater state changes
	@Override
	public void update(Observable o, Object arg) {
		if (configuration.isAutoUpdate()) {
			checkForUpdates(true);
		}
	}

	/**
	 * Start the process of checking for updates.
	 *
	 * @param isStartup whether this is being called via startup or button
	 */
	public void checkForUpdates(boolean isStartup) {
		if (autoUpdater != null) {
			try {
				AutoUpdateDialog.showIfNecessary(this, autoUpdater, isStartup);
			} catch (NoClassDefFoundError ncdfe) {
				LOGGER.error("Error displaying AutoUpdateDialog", ncdfe);
			}
		}
	}

	@Override
	public void setStatusLine(String line) {
		if (line == null || "".equals(line)) {
			line = "";
			status.setBorder(BorderFactory.createEmptyBorder());
		} else {
			status.setBorder(BorderFactory.createEmptyBorder(0, 9, 8, 0));
		}

		status.setText(line);
	}

	@Override
	public void addRenderer(RendererConfiguration renderer) {
		st.addRenderer(renderer);
	}

	@Override
	public void updateRenderer(RendererConfiguration renderer) {
		StatusTab.updateRenderer(renderer);
	}

	@Override
	public void serverReady() {
		st.updateMemoryUsage();
		gt.addRenderers();
		pt.addPlugins();
	}

	@Override
	public void setScanLibraryEnabled(boolean flag) {
		getNt().setScanLibraryEnabled(flag);
	}

	@Override
	public String getLog() {
		return getTt().getList().getText();
	}
}
