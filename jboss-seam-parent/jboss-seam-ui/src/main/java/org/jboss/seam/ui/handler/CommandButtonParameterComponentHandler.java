package org.jboss.seam.ui.handler;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;

import static org.jboss.seam.ui.util.cdk.RendererBase.getUtils;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;

/**
 * If user didn't specify id on button, do it for them (we need one rendered)
 * 
 * @author Pete Muir
 *
 */
public class CommandButtonParameterComponentHandler extends ComponentHandler
{
   
   public CommandButtonParameterComponentHandler(ComponentConfig config)
   {
      super(config);
   }
   
   @Override
   public void onComponentCreated(FaceletContext ctx, UIComponent c,
         UIComponent parent)
   {
      if (getUtils().isCommandButton(parent) && parent.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      {
         parent.setId("seam" + parent.getId());
      }
   }

}
