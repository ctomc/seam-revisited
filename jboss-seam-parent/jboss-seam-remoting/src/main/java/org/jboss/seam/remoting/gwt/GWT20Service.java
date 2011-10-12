package org.jboss.seam.remoting.gwt;

import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.HasValues;
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.rpc.client.ast.ThrowCommand;
import com.google.gwt.rpc.client.impl.HasValuesCommandSink;
import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.rpc.server.CommandServerSerializationStreamWriter;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;
import com.google.gwt.user.server.rpc.impl.TypeNameObfuscator;

@Name("org.jboss.seam.remoting.gwt.gwt2RemoteService")
@Scope(ScopeType.APPLICATION)
@Install(precedence = BUILT_IN, classDependencies = { "com.google.gwt.user.client.rpc.RemoteService" })
@BypassInterceptors
public class GWT20Service extends GWTService
{
   private static final HashMap<String, Class<?>> TYPE_NAMES = new HashMap<String, Class<?>>();

   static
   {
      // The space is needed to prevent name collisions
      TYPE_NAMES.put(" Z", boolean.class);
      TYPE_NAMES.put(" B", byte.class);
      TYPE_NAMES.put(" C", char.class);
      TYPE_NAMES.put(" D", double.class);
      TYPE_NAMES.put(" F", float.class);
      TYPE_NAMES.put(" I", int.class);
      TYPE_NAMES.put(" J", long.class);
      TYPE_NAMES.put(" S", short.class);
   }

   @Create
   public void create()
   {

   }

   @Override
   public String processCall(String payload) throws SerializationException
   {
      try
      {
         SeamRPCRequest rpcRequest = RPC_decodeRequest(payload, this.getClass(), this);

         return RPC_invokeAndEncodeResponse(this, rpcRequest.getMethod(), rpcRequest.getParameterTypes(), rpcRequest.getParameters(), rpcRequest.getSerializationPolicy());
      }
      catch (IncompatibleRemoteServiceException ex)
      {
         getServletContext().log("An IncompatibleRemoteServiceException was thrown while processing this call.", ex);
         return RPC.encodeResponseForFailure(null, ex);
      }

   }

   @Override
   protected String createResponse(ServerSerializationStreamWriter stream, Class responseType, Object responseObj, boolean isException)
   {
      return null;
   }

   @Override
   protected ServerSerializationStreamReader getStreamReader()
   {
      return null;
   }

   @Override
   protected ServerSerializationStreamWriter getStreamWriter()
   {
      return null;
   }

   @Override
   public String getResourcePath()
   {
      return "/gwt2";
   }

   public static void RPC_invokeAndStreamResponse(Object target, Method serviceMethod, Object[] args, ClientOracle clientOracle, OutputStream stream) throws SerializationException
   {
      if (serviceMethod == null)
      {
         throw new NullPointerException("serviceMethod");
      }

      if (clientOracle == null)
      {
         throw new NullPointerException("clientOracle");
      }

      CommandSink sink;
      try
      {
         sink = clientOracle.createCommandSink(stream);
      }
      catch (IOException e)
      {
         throw new SerializationException("Unable to initialize output", e);
      }

      try
      {
         GWTToSeamAdapter adapter = GWTToSeamAdapter.instance();
         String serviceIntfName = serviceMethod.getDeclaringClass().getName();

         GWTToSeamAdapter.ReturnedObject returnedObject = adapter.callWebRemoteMethod(serviceIntfName, serviceMethod.getName(), serviceMethod.getParameterTypes(), args);

         // Object result = serviceMethod.invoke(target, args);

         try
         {
            RPC_streamResponse(clientOracle, returnedObject.returnedObject, sink, false);
         }
         catch (SerializationException e)
         {
            RPC_streamResponse(clientOracle, e, sink, true);
         }

      }
      catch (IllegalAccessException e)
      {
         SecurityException securityException = new SecurityException(RPC_formatIllegalAccessErrorMessage(target, serviceMethod));
         securityException.initCause(e);
         throw securityException;
      }
      catch (IllegalArgumentException e)
      {
         SecurityException securityException = new SecurityException(RPC_formatIllegalArgumentErrorMessage(target, serviceMethod, args));
         securityException.initCause(e);
         throw securityException;
      }
      catch (InvocationTargetException e)
      {
         // Try to encode the caught exception
         Throwable cause = e.getCause();

         // Don't allow random RuntimeExceptions to be thrown back to the client
         if (!RPCServletUtils.isExpectedException(serviceMethod, cause))
         {
            throw new UnexpectedException("Service method '" + RPC_getSourceRepresentation(serviceMethod) + "' threw an unexpected exception: " + cause.toString(), cause);
         }

         RPC_streamResponse(clientOracle, cause, sink, true);
      }
      sink.finish();
   }

   private static void RPC_streamResponse(ClientOracle clientOracle, Object payload, CommandSink sink, boolean asThrow) throws SerializationException
   {
      HasValues command;
      if (asThrow)
      {
         command = new ThrowCommand();
         assert payload instanceof Throwable : "Trying to throw something other than a Throwable";
         // payload = new RemoteException((Throwable) payload);
      }
      else
      {
         command = new ReturnCommand();
      }

      CommandServerSerializationStreamWriter out = new CommandServerSerializationStreamWriter(clientOracle, new HasValuesCommandSink(command));

      out.writeObject(payload);

      sink.accept((RpcCommand) command);
   }

   private static String RPC_formatIllegalAccessErrorMessage(Object target, Method serviceMethod)
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Blocked attempt to access inaccessible method '");
      sb.append(RPC_getSourceRepresentation(serviceMethod));
      sb.append("'");

      if (target != null)
      {
         sb.append(" on target '");
         sb.append(TypeInfo_printTypeName(target.getClass()));
         sb.append("'");
      }

      sb.append("; this is either misconfiguration or a hack attempt");

      return sb.toString();
   }

   private static String RPC_formatIllegalArgumentErrorMessage(Object target, Method serviceMethod, Object[] args)
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Blocked attempt to invoke method '");
      sb.append(RPC_getSourceRepresentation(serviceMethod));
      sb.append("'");

      if (target != null)
      {
         sb.append(" on target '");
         sb.append(TypeInfo_printTypeName(target.getClass()));
         sb.append("'");
      }

      sb.append(" with invalid arguments");

      if (args != null && args.length > 0)
      {
         sb.append(Arrays.asList(args));
      }

      return sb.toString();
   }

   /**
    * Returns the source representation for a method signature.
    *
    * @param method
    *           method to get the source signature for
    * @return source representation for a method signature
    */
   private static String RPC_getSourceRepresentation(Method method)
   {
      return method.toString().replace('$', '.');
   }

   /**
    * Straight copy from
    * {@link com.google.gwt.dev.util.TypeInfo#getSourceRepresentation(Class)} to
    * avoid runtime dependency on gwt-dev.
    */
   private static String TypeInfo_printTypeName(Class<?> type)
   {
      // Primitives
      //
      if (type.equals(Integer.TYPE))
      {
         return "int";
      }
      else if (type.equals(Long.TYPE))
      {
         return "long";
      }
      else if (type.equals(Short.TYPE))
      {
         return "short";
      }
      else if (type.equals(Byte.TYPE))
      {
         return "byte";
      }
      else if (type.equals(Character.TYPE))
      {
         return "char";
      }
      else if (type.equals(Boolean.TYPE))
      {
         return "boolean";
      }
      else if (type.equals(Float.TYPE))
      {
         return "float";
      }
      else if (type.equals(Double.TYPE))
      {
         return "double";
      }

      // Arrays
      //
      if (type.isArray())
      {
         Class<?> componentType = type.getComponentType();
         return TypeInfo_printTypeName(componentType) + "[]";
      }

      // Everything else
      //
      return type.getName().replace('$', '.');
   }

   public static SeamRPCRequest RPC_decodeRequest(String encodedRequest, Class<?> type, SerializationPolicyProvider serializationPolicyProvider)
   {
      if (encodedRequest == null)
      {
         throw new NullPointerException("encodedRequest cannot be null");
      }

      if (encodedRequest.length() == 0)
      {
         throw new IllegalArgumentException("encodedRequest cannot be empty");
      }

      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      try
      {
         ServerSerializationStreamReader streamReader = new ServerSerializationStreamReader(classLoader, serializationPolicyProvider);
         streamReader.prepareToRead(encodedRequest);

         // Read the name of the RemoteService interface
         String serviceIntfName = RPC_maybeDeobfuscate(streamReader, streamReader.readString());

         // if (type != null) {
         // if (!implementsInterface(type, serviceIntfName)) {
         // // The service does not implement the requested interface
         // throw new IncompatibleRemoteServiceException(
         // "Blocked attempt to access interface '" + serviceIntfName
         // + "', which is not implemented by '" + TypeInfo_printTypeName(type)
         // + "'; this is either misconfiguration or a hack attempt");
         // }
         // }

         SerializationPolicy serializationPolicy = streamReader.getSerializationPolicy();
         Class<?> serviceIntf;
         try
         {
            serviceIntf = RPC_getClassFromSerializedName(serviceIntfName, classLoader);
            if (!RemoteService.class.isAssignableFrom(serviceIntf))
            {
               // The requested interface is not a RemoteService interface
               throw new IncompatibleRemoteServiceException("Blocked attempt to access interface '" + TypeInfo_printTypeName(serviceIntf) + "', which doesn't extend RemoteService; this is either misconfiguration or a hack attempt");
            }
         }
         catch (ClassNotFoundException e)
         {
            throw new IncompatibleRemoteServiceException("Could not locate requested interface '" + serviceIntfName + "' in default classloader", e);
         }

         String serviceMethodName = streamReader.readString();

         int paramCount = streamReader.readInt();
         if (paramCount > streamReader.getNumberOfTokens())
         {
            throw new IncompatibleRemoteServiceException("Invalid number of parameters");
         }
         Class<?>[] parameterTypes = new Class[paramCount];

         for (int i = 0; i < parameterTypes.length; i++)
         {
            String paramClassName = RPC_maybeDeobfuscate(streamReader, streamReader.readString());

            try
            {
               parameterTypes[i] = RPC_getClassFromSerializedName(paramClassName, classLoader);
            }
            catch (ClassNotFoundException e)
            {
               throw new IncompatibleRemoteServiceException("Parameter " + i + " of is of an unknown type '" + paramClassName + "'", e);
            }
         }

         try
         {
            Method method = serviceIntf.getMethod(serviceMethodName, parameterTypes);

            Object[] parameterValues = new Object[parameterTypes.length];
            for (int i = 0; i < parameterValues.length; i++)
            {
               parameterValues[i] = streamReader.deserializeValue(parameterTypes[i]);
            }

            return new SeamRPCRequest(method, parameterValues, method.getParameterTypes(), serializationPolicy);

         }
         catch (NoSuchMethodException e)
         {
            throw new IncompatibleRemoteServiceException(formatMethodNotFoundErrorMessage(serviceIntf, serviceMethodName, parameterTypes));
         }
      }
      catch (SerializationException ex)
      {
         throw new IncompatibleRemoteServiceException(ex.getMessage(), ex);
      }
   }

   private static String formatMethodNotFoundErrorMessage(Class<?> serviceIntf, String serviceMethodName, Class<?>[] parameterTypes)
   {
      StringBuffer sb = new StringBuffer();

      sb.append("Could not locate requested method '");
      sb.append(serviceMethodName);
      sb.append("(");
      for (int i = 0; i < parameterTypes.length; ++i)
      {
         if (i > 0)
         {
            sb.append(", ");
         }
         sb.append(TypeInfo_printTypeName(parameterTypes[i]));
      }
      sb.append(")'");

      sb.append(" in interface '");
      sb.append(TypeInfo_printTypeName(serviceIntf));
      sb.append("'");

      return sb.toString();
   }

   /**
    * Given a type identifier in the stream, attempt to deobfuscate it. Retuns
    * the original identifier if deobfuscation is unnecessary or no mapping is
    * known.
    */
   private static String RPC_maybeDeobfuscate(ServerSerializationStreamReader streamReader, String name) throws SerializationException
   {
      int index;
      if (streamReader.hasFlags(AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES))
      {
         SerializationPolicy serializationPolicy = streamReader.getSerializationPolicy();
         if (!(serializationPolicy instanceof TypeNameObfuscator))
         {
            throw new IncompatibleRemoteServiceException("RPC request was encoded with obfuscated type names, " + "but the SerializationPolicy in use does not implement " + TypeNameObfuscator.class.getName());
         }

         String maybe = ((TypeNameObfuscator) serializationPolicy).getClassNameForTypeId(name);
         if (maybe != null)
         {
            return maybe;
         }
      }
      else if ((index = name.indexOf('/')) != -1)
      {
         return name.substring(0, index);
      }
      return name;
   }

   /**
    * Returns the {@link Class} instance for the named class or primitive type.
    *
    * @param serializedName
    *           the serialized name of a class or primitive type
    * @param classLoader
    *           the classLoader used to load {@link Class}es
    * @return Class instance for the given type name
    * @throws ClassNotFoundException
    *            if the named type was not found
    */
   private static Class<?> RPC_getClassFromSerializedName(String serializedName, ClassLoader classLoader) throws ClassNotFoundException
   {
      Class<?> value = TYPE_NAMES.get(serializedName);
      if (value != null)
      {
         return value;
      }

      return Class.forName(serializedName, false, classLoader);
   }
}
