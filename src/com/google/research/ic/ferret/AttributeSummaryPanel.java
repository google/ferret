package com.google.research.ic.ferret;

import com.google.research.ic.ferret.data.attributes.Bin;

import java.awt.Dimension;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Summarizes the distribution for a single attribute (multiple bins)
 */
public class AttributeSummaryPanel extends JPanel{
  
  public AttributeSummaryPanel(List<Bin> bins){

    double maxValue = 0.0;
    for (Bin b : bins) {
      double c = b.getCount();
      if (c > maxValue) {
        maxValue = c;
      }
    }
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    JPanel rowPanel = null;    
    JLabel binName = null;
    JPanel barPanel = null;
    JLabel countLabel = null;
    
    for (Bin b : bins) {
      rowPanel = new JPanel();
      rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
      binName = new JLabel(b.getDisplayName());
      binName.setAlignmentX(LEFT_ALIGNMENT);
      barPanel = new BarPanel(b.getCount(), maxValue * 1.25);
      countLabel = new JLabel("" + b.getCount());
      rowPanel.add(binName);
      rowPanel.add(barPanel);
      rowPanel.add(countLabel);
      add(rowPanel);
    }    
    setPreferredSize(new Dimension(100, 400));
  }
  
}
