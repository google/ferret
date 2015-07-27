/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
