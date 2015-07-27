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

import com.google.research.ic.ferret.data.Event;
import com.google.research.ic.ferret.test.Debug;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.StringWriter;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EventPanel extends JPanel {
	
  public static final Dimension DEFAULT_DIMENSION = new Dimension(100,100);

  private Image backgroundImage = null;
  
  public EventPanel(Event event) {      

    backgroundImage = 
        ResourceManager.getResourceManager().getEventBackgroundImageIcon().getImage();
    
    Dimension theDimension = DEFAULT_DIMENSION;
    setMinimumSize(theDimension);
    setMaximumSize(theDimension);
    setPreferredSize(theDimension);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    String labelText = null;

    labelText = formatLabelText(event.getModuleDescription(), "white", "normal", "20px", "5px");
    JLabel modLabel = new JLabel(labelText);
    modLabel.setOpaque(false);
    
    labelText = formatLabelText(event.getEventTypeName(), "black", "large", "15px", "3px");
    JLabel typeLabel = new JLabel(labelText);
    typeLabel.setOpaque(false);


    labelText = String.format("<html><div style=\"width:%dpx;font-size:x-small\">%s</div><html>", 80, event.getComponentDescription());
    labelText = formatLabelText(event.getComponentDescription(), "black", "x-small", "10px",  "1px");
    JLabel descLabel = new JLabel(labelText);
    descLabel.setOpaque(false);

    labelText = String.format("<html><div style=\"width:%dpx;font-size:x-small\">\"%s\"</div><html>", 80, event.getComponentInfo());
    labelText = formatLabelText(event.getComponentInfo(), "black", "x-small", "10px", "1px");
    JLabel infoLabel = new JLabel(labelText);
    infoLabel.setOpaque(false);

    add(modLabel);
    add(typeLabel);
    add(descLabel);
    add(infoLabel);
//    setBorder(BorderFactory.createLineBorder(Color.black));
    invalidate();
  }

  private String formatLabelText(String text, String color, String size, String height, String vPad) {
    StringWriter sw = new StringWriter();
    sw.append("<html><div style=\"");
    sw.append("width:" + DEFAULT_DIMENSION.width);
    sw.append(";height:" + height);
    sw.append(";text-align:center");
    sw.append(";padding:" + vPad + " 1px");
    sw.append(";color:" + color);
    sw.append(";font-size:" + size);
    sw.append(";font-family:Verdana, Geneva, sans-serif");
    sw.append("\">");
    sw.append(text);
    sw.append("</div><html>");
    
    return sw.toString();
    
  }
  
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(backgroundImage, 0, 0, this);
  }
  
}
