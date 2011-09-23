package org.jboss.seam.ui.facelet;

import static org.jboss.seam.ScopeType.APPLICATION;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
//import org.jboss.seam.log.LogProvider;
//import org.jboss.seam.log.Logging;

import com.sun.faces.application.ApplicationAssociate;

@Name("org.jboss.seam.ui.facelet.faceletCompiler")
@Scope(APPLICATION)
@BypassInterceptors
@AutoCreate
@Install(value = true, precedence = Install.BUILT_IN, classDependencies="com.sun.faces.facelets.Facelet")
public class FaceletCompiler
{
   
  // private LogProvider log = Logging.getLogProvider(FaceletCompiler.class);
   private com.sun.faces.facelets.compiler.Compiler compiler;
   
   @Create
   public void create()
   {
      compiler = ApplicationAssociate.getCurrentInstance().getCompiler();
   }
   
   @Unwrap
   public com.sun.faces.facelets.compiler.Compiler unwrap()
   {
      return compiler;
   }
   
   public static com.sun.faces.facelets.compiler.Compiler instance()
   {
      if ( !Contexts.isApplicationContextActive() )
      {
         throw new IllegalStateException("No active application scope");
      }
      return (com.sun.faces.facelets.compiler.Compiler) Component.getInstance(FaceletCompiler.class, ScopeType.APPLICATION);
   }
   
}
