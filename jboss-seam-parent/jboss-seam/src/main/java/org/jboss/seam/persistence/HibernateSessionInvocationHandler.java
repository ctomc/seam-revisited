package org.jboss.seam.persistence;

import org.hibernate.*;
import org.hibernate.Filter;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.ActionQueue;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;*/
//import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
//import org.hibernate.event.EventListeners;
//import org.hibernate.event.EventSource;
//import org.hibernate.impl.CriteriaImpl;
//import org.hibernate.jdbc.Batcher;
//import org.hibernate.jdbc.JDBCContext;

/**
 * InvocationHandler that proxies the Session, and implements EL interpolation
 * in HQL. Needs to implement SessionImplementor because DetachedCriteria casts
 * the Session to SessionImplementor.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Mike Youngstrom
 * @author Marek Novotny
 */
public class HibernateSessionInvocationHandler implements InvocationHandler, Serializable, EventSource {

    private SessionImpl delegate;

    public HibernateSessionInvocationHandler(Session paramDelegate) {
        this.delegate = (SessionImpl) paramDelegate;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0 && method.getParameterTypes()[0].equals(String.class)) {
                return handleCreateQueryWithString(method, args);
            }
            if ("reconnect".equals(method.getName()) && method.getParameterTypes().length == 0) {
                return handleReconnectNoArg(method);
            }
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    protected Object handleCreateQueryWithString(Method method, Object[] args) throws Throwable {
        if (args[0] == null) {
            //return method.invoke(getDelegate(method), args);
            return method.invoke(delegate, args);
        }
        String ejbql = (String) args[0];
        if (ejbql.indexOf('#') > 0) {
            QueryParser qp = new QueryParser(ejbql);
            Object[] newArgs = args.clone();
            newArgs[0] = qp.getEjbql();
            //Query query = (Query) method.invoke(getDelegate(method), newArgs);
            Query query = (Query) method.invoke(delegate, newArgs);
            for (int i = 0; i < qp.getParameterValueBindings().size(); i++) {
                query.setParameter(QueryParser.getParameterName(i), qp.getParameterValueBindings().get(i).getValue());
            }
            return query;
        } else {
            return method.invoke(delegate, args);
        }
    }

    protected Object handleReconnectNoArg(Method method) throws Throwable {
        throw new UnsupportedOperationException("deprecated");
    }

    public Interceptor getInterceptor() {
        return ((SessionImplementor) delegate).getInterceptor();
    }

    public void setAutoClear(boolean paramBoolean) {
        ((SessionImplementor) delegate).setAutoClear(paramBoolean);
    }

    public boolean isTransactionInProgress() {
        return ((SessionImplementor) delegate).isTransactionInProgress();
    }

    public void initializeCollection(PersistentCollection paramPersistentCollection, boolean paramBoolean) throws HibernateException {
        ((SessionImplementor) delegate).initializeCollection(paramPersistentCollection, paramBoolean);
    }

    public Object internalLoad(String paramString, Serializable paramSerializable, boolean paramBoolean1, boolean paramBoolean2) throws HibernateException {
        return ((SessionImplementor) delegate).internalLoad(paramString, paramSerializable, paramBoolean1, paramBoolean2);
    }

    public Object immediateLoad(String paramString, Serializable paramSerializable) throws HibernateException {
        return ((SessionImplementor) delegate).immediateLoad(paramString, paramSerializable);
    }

    public long getTimestamp() {
        return ((SessionImplementor) delegate).getTimestamp();
    }

    public SessionFactoryImplementor getFactory() {
        return ((SessionImplementor) delegate).getFactory();
    }
/*
   public Batcher getBatcher()
   {
      return ((SessionImplementor) delegate).getBatcher();
   }*/

    public List list(String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).list(paramString, paramQueryParameters);
    }

    public Iterator iterate(String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).iterate(paramString, paramQueryParameters);
    }

    public ScrollableResults scroll(String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).scroll(paramString, paramQueryParameters);
    }

    public ScrollableResults scroll(CriteriaImpl paramCriteriaImpl, ScrollMode paramScrollMode) {
        return ((SessionImplementor) delegate).scroll(paramCriteriaImpl, paramScrollMode);
    }

    public List list(CriteriaImpl paramCriteriaImpl) {
        return ((SessionImplementor) delegate).list(paramCriteriaImpl);
    }

    public List listFilter(Object paramObject, String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).listFilter(paramObject, paramString, paramQueryParameters);
    }

    public Iterator iterateFilter(Object paramObject, String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).iterateFilter(paramObject, paramString, paramQueryParameters);
    }

    public EntityPersister getEntityPersister(String paramString, Object paramObject) throws HibernateException {
        return ((SessionImplementor) delegate).getEntityPersister(paramString, paramObject);
    }

    public Object getEntityUsingInterceptor(EntityKey paramEntityKey) throws HibernateException {
        return ((SessionImplementor) delegate).getEntityUsingInterceptor(paramEntityKey);
    }

    /* public void afterTransactionCompletion(boolean paramBoolean, Transaction paramTransaction)
    {
       ((SessionImplementor) delegate).afterTransactionCompletion(paramBoolean, paramTransaction);
    }

    public void beforeTransactionCompletion(Transaction paramTransaction)
    {
       ((SessionImplementor) delegate).beforeTransactionCompletion(paramTransaction)      ;
    }*/

    public Serializable getContextEntityIdentifier(Object paramObject) {
        return ((SessionImplementor) delegate).getContextEntityIdentifier(paramObject);
    }

    public String bestGuessEntityName(Object paramObject) {
        return ((SessionImplementor) delegate).bestGuessEntityName(paramObject);
    }

    public String guessEntityName(Object paramObject) throws HibernateException {
        return ((SessionImplementor) delegate).guessEntityName(paramObject);
    }

    public Object instantiate(String paramString, Serializable paramSerializable) throws HibernateException {
        return ((SessionImplementor) delegate).instantiate(paramString, paramSerializable);
    }

    public List listCustomQuery(CustomQuery paramCustomQuery, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).listCustomQuery(paramCustomQuery, paramQueryParameters);
    }

    public ScrollableResults scrollCustomQuery(CustomQuery paramCustomQuery, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).scrollCustomQuery(paramCustomQuery, paramQueryParameters);
    }

    public List list(NativeSQLQuerySpecification paramNativeSQLQuerySpecification, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).list(paramNativeSQLQuerySpecification, paramQueryParameters);
    }

    public ScrollableResults scroll(NativeSQLQuerySpecification paramNativeSQLQuerySpecification, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).scroll(paramNativeSQLQuerySpecification, paramQueryParameters);
    }

    public Object getFilterParameterValue(String paramString) {
        return ((SessionImplementor) delegate).getFilterParameterValue(paramString);
    }

    public Type getFilterParameterType(String paramString) {
        return ((SessionImplementor) delegate).getFilterParameterType(paramString);
    }

    public Map getEnabledFilters() {
        return ((SessionImplementor) delegate).getEnabledFilters();
    }

    public int getDontFlushFromFind() {
        return ((SessionImplementor) delegate).getDontFlushFromFind();
    }

    /*public EventListeners getListeners()
    {
       return ((SessionImplementor) delegate).getListeners();
    }*/

    public PersistenceContext getPersistenceContext() {
        return ((SessionImplementor) delegate).getPersistenceContext();
    }

    public int executeUpdate(String paramString, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).executeUpdate(paramString, paramQueryParameters);
    }

    public int executeNativeUpdate(NativeSQLQuerySpecification paramNativeSQLQuerySpecification, QueryParameters paramQueryParameters) throws HibernateException {
        return ((SessionImplementor) delegate).executeNativeUpdate(paramNativeSQLQuerySpecification, paramQueryParameters);
    }


    /*  public EntityMode getEntityMode()
    {
       return ((SessionImplementor) delegate).getEntityMode();
    }*/

    @Override
    public CacheMode getCacheMode() {
        return ((SessionImplementor) delegate).getCacheMode();
    }

    @Override
    public void setCacheMode(CacheMode paramCacheMode) {
        ((SessionImplementor) delegate).setCacheMode(paramCacheMode);
    }

    @Override
    public boolean isOpen() {
        return ((SessionImplementor) delegate).isOpen();
    }

    @Override
    public boolean isConnected() {
        return ((SessionImplementor) delegate).isConnected();
    }

    @Override
    public FlushMode getFlushMode() {
        return ((SessionImplementor) delegate).getFlushMode();
    }

    @Override
    public void setFlushMode(FlushMode paramFlushMode) {
        ((SessionImplementor) delegate).setFlushMode(paramFlushMode);
    }

    public Connection connection() {
        return ((SessionImplementor) delegate).connection();
    }

    @Override
    public void flush() {
        ((SessionImplementor) delegate).flush();
    }

    @Override
    public Query getNamedQuery(String paramString) {
        return ((SessionImplementor) delegate).getNamedQuery(paramString);
    }

    @Override
    public String getTenantIdentifier() {
        return delegate.getTenantIdentifier();
    }

    public Query getNamedSQLQuery(String paramString) {
        return ((SessionImplementor) delegate).getNamedSQLQuery(paramString);
    }

    public boolean isEventSource() {
        return ((SessionImplementor) delegate).isEventSource();
    }

    public void afterScrollOperation() {
        ((SessionImplementor) delegate).afterScrollOperation();
    }

    public String getFetchProfile() {
        return ((SessionImplementor) delegate).getFetchProfile();
    }

    public void setFetchProfile(String paramString) {
        ((SessionImplementor) delegate).setFetchProfile(paramString);
    }

/*
   public JDBCContext getJDBCContext()
   {
      return ((SessionImplementor) delegate).getJDBCContext();
   }
*/

    public boolean isClosed() {
        return ((SessionImplementor) delegate).isClosed();
    }

    /*  public Session getSession(EntityMode paramEntityMode)
    {
       return delegate.getSession(paramEntityMode);
    }*/

    @Override
    public SessionFactory getSessionFactory() {
        return delegate.getSessionFactory();
    }

    @Override
    public Connection close() throws HibernateException {
        return delegate.close();
    }

    @Override
    public void cancelQuery() throws HibernateException {
        delegate.cancelQuery();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        return delegate.isDirty();
    }

    @Override
    public boolean isDefaultReadOnly() {
        return delegate.isDefaultReadOnly();
    }

    @Override
    public void setDefaultReadOnly(boolean paramBoolean) {
        delegate.setDefaultReadOnly(paramBoolean);
    }

    @Override
    public Serializable getIdentifier(Object paramObject) throws HibernateException {
        return delegate.getIdentifier(paramObject);
    }

    @Override
    public LobHelper getLobHelper() {
        return delegate.getLobHelper();
    }

    @Override
    public boolean contains(Object paramObject) {
        return delegate.contains(paramObject);
    }

    @Override
    public void evict(Object paramObject) throws HibernateException {
        delegate.evict(paramObject);
    }

    @Override
    @Deprecated
    public Object load(Class paramClass, Serializable paramSerializable, LockMode paramLockMode) throws HibernateException {
        return delegate.load(paramClass, paramSerializable, paramLockMode);
    }

    @Override
    public Object load(Class aClass, Serializable serializable, LockOptions lockOptions) throws HibernateException {
        return delegate.load(aClass, serializable, lockOptions);
    }

    @Override
    @Deprecated
    public Object load(String paramString, Serializable paramSerializable, LockMode paramLockMode) throws HibernateException {
        return delegate.load(paramString, paramSerializable, paramLockMode);
    }

    @Override
    public Object load(String s, Serializable serializable, LockOptions lockOptions) throws HibernateException {
        return delegate.load(s, serializable, lockOptions);
    }

    @Override
    public Object load(Class paramClass, Serializable paramSerializable) throws HibernateException {
        return delegate.load(paramClass, paramSerializable);
    }

    @Override
    public Object load(String paramString, Serializable paramSerializable) throws HibernateException {
        return delegate.load(paramString, paramSerializable);
    }

    @Override
    public void load(Object paramObject, Serializable paramSerializable) throws HibernateException {
        delegate.load(paramObject, paramSerializable);
    }

    @Override
    public void replicate(Object paramObject, ReplicationMode paramReplicationMode) throws HibernateException {
        delegate.replicate(paramObject, paramReplicationMode);
    }

    @Override
    public void replicate(String paramString, Object paramObject, ReplicationMode paramReplicationMode) throws HibernateException {
        delegate.replicate(paramString, paramObject, paramReplicationMode);
    }

    @Override
    public Serializable save(Object paramObject) throws HibernateException {
        return delegate.save(paramObject);
    }

    @Override
    public Serializable save(String paramString, Object paramObject) throws HibernateException {
        return delegate.save(paramString, paramObject);
    }

    @Override
    public void saveOrUpdate(Object paramObject) throws HibernateException {
        delegate.saveOrUpdate(paramObject);
    }

    @Override
    public void saveOrUpdate(String paramString, Object paramObject) throws HibernateException {
        delegate.saveOrUpdate(paramString, paramObject);
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        return delegate.sessionWithOptions();
    }

    @Override
    public void update(Object paramObject) throws HibernateException {
        delegate.update(paramObject);
    }

    @Override
    public void update(String paramString, Object paramObject) throws HibernateException {
        delegate.update(paramString, paramObject);
    }

    @Override
    public Object merge(Object paramObject) throws HibernateException {
        return delegate.merge(paramObject);
    }

    @Override
    public Object merge(String paramString, Object paramObject) throws HibernateException {
        return delegate.merge(paramString, paramObject);
    }

    @Override
    public void persist(Object paramObject) throws HibernateException {
        delegate.persist(paramObject);
    }

    @Override
    public void persist(String paramString, Object paramObject) throws HibernateException {
        delegate.persist(paramString, paramObject);
    }

    @Override
    public void delete(Object paramObject) throws HibernateException {
        delegate.delete(paramObject);
    }

    @Override
    public void delete(String paramString, Object paramObject) throws HibernateException {
        ((EventSource) delegate).delete(paramString, paramObject);
    }

    @Override
    @Deprecated
    public void lock(Object paramObject, LockMode paramLockMode) throws HibernateException {
        delegate.lock(paramObject, paramLockMode);
    }

    @Override
    @Deprecated
    public void lock(String paramString, Object paramObject, LockMode paramLockMode) throws HibernateException {
        delegate.lock(paramString, paramObject, paramLockMode);
    }

    @Override
    public void refresh(Object paramObject) throws HibernateException {
        delegate.refresh(paramObject);
    }

    @Override
    @Deprecated
    public void refresh(Object paramObject, LockMode paramLockMode) throws HibernateException {
        delegate.refresh(paramObject, paramLockMode);
    }

    @Override
    public void refresh(Object o, LockOptions lockOptions) throws HibernateException {
        delegate.refresh(o, lockOptions);
    }

    @Override
    public void refresh(String s, Object o) throws HibernateException {
        delegate.refresh(s, o);
    }

    @Override
    public void refresh(String s, Object o, LockOptions lockOptions) throws HibernateException {
        delegate.refresh(s, o, lockOptions);
    }

    @Override
    public LockMode getCurrentLockMode(Object paramObject) throws HibernateException {
        return delegate.getCurrentLockMode(paramObject);
    }

    @Override
    public Filter getEnabledFilter(String s) {
        return delegate.getEnabledFilter(s);
    }

    @Override
    public Transaction beginTransaction() throws HibernateException {
        return delegate.beginTransaction();
    }

    @Override
    public Transaction getTransaction() {
        return delegate.getTransaction();
    }

    @Override
    public Criteria createCriteria(Class paramClass) {
        return delegate.createCriteria(paramClass);
    }

    @Override
    public Criteria createCriteria(Class paramClass, String paramString) {
        return delegate.createCriteria(paramClass, paramString);
    }

    @Override
    public Criteria createCriteria(String paramString) {
        return delegate.createCriteria(paramString);
    }

    @Override
    public Criteria createCriteria(String paramString1, String paramString2) {
        return delegate.createCriteria(paramString1, paramString2);
    }

    @Override
    public Query createQuery(String paramString) throws HibernateException {
        return delegate.createQuery(paramString);
    }

    @Override
    public SQLQuery createSQLQuery(String paramString) throws HibernateException {
        return delegate.createSQLQuery(paramString);
    }

    @Override
    public Query createFilter(Object paramObject, String paramString) throws HibernateException {
        return delegate.createFilter(paramObject, paramString);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Object get(Class paramClass, Serializable paramSerializable) throws HibernateException {
        return delegate.get(paramClass, paramSerializable);
    }

    @Override
    @Deprecated
    public Object get(Class paramClass, Serializable paramSerializable, LockMode paramLockMode) throws HibernateException {
        return delegate.get(paramClass, paramSerializable, paramLockMode);
    }

    @Override
    public Object get(Class aClass, Serializable serializable, LockOptions lockOptions) throws HibernateException {
        return delegate.get(aClass, serializable, lockOptions);
    }

    @Override
    public Object get(String paramString, Serializable paramSerializable) throws HibernateException {
        return delegate.get(paramString, paramSerializable);
    }

    @Override
    @Deprecated
    public Object get(String paramString, Serializable paramSerializable, LockMode paramLockMode) throws HibernateException {
        return delegate.get(paramString, paramSerializable, paramLockMode);
    }

    @Override
    public Object get(String s, Serializable serializable, LockOptions lockOptions) throws HibernateException {
        return delegate.get(s, serializable, lockOptions);
    }

    @Override
    public String getEntityName(Object paramObject) throws HibernateException {
        return delegate.getEntityName(paramObject);
    }


    @Override
    public void disableFilter(String paramString) {
        delegate.disableFilter(paramString);
    }

    @Override
    public SessionStatistics getStatistics() {
        return delegate.getStatistics();
    }

    @Override
    public TypeHelper getTypeHelper() {
        return delegate.getTypeHelper();
    }

    @Override
    public boolean isReadOnly(Object paramObject) {
        return delegate.isReadOnly(paramObject);
    }

    @Override
    public void setReadOnly(Object paramObject, boolean paramBoolean) {
        delegate.setReadOnly(paramObject, paramBoolean);
    }

    @Override
    public void doWork(Work paramWork) throws HibernateException {
        delegate.doWork(paramWork);
    }

    @Override
    public Connection disconnect() throws HibernateException {
        return delegate.disconnect();
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> tReturningWork) throws HibernateException {
        return delegate.doReturningWork(tReturningWork);
    }

    /* @SuppressWarnings("deprecation")
    public void reconnect() throws HibernateException
    {
       delegate.reconnect();
    }*/

    @Override
    public void reconnect(Connection paramConnection) throws HibernateException {
        delegate.reconnect(paramConnection);
    }

    @Override
    public boolean isFetchProfileEnabled(String paramString) {
        return delegate.isFetchProfileEnabled(paramString);
    }

    @Override
    public void enableFetchProfile(String paramString) {
        delegate.enableFetchProfile(paramString);
    }

    @Override
    public Filter enableFilter(String s) {
        return delegate.enableFilter(s);
    }

    @Override
    public void disableFetchProfile(String paramString) {
        delegate.disableFetchProfile(paramString);
    }

    public ActionQueue getActionQueue() {
        return ((EventSource) delegate).getActionQueue();
    }

    public Object instantiate(EntityPersister paramEntityPersister, Serializable paramSerializable) throws HibernateException {
        return ((EventSource) delegate).instantiate(paramEntityPersister, paramSerializable);
    }

    public void forceFlush(EntityEntry paramEntityEntry) throws HibernateException {
        ((EventSource) delegate).forceFlush(paramEntityEntry);
    }

    public void merge(String paramString, Object paramObject, Map paramMap) throws HibernateException {
        ((EventSource) delegate).merge(paramString, paramObject, paramMap);
    }

    public void persist(String paramString, Object paramObject, Map paramMap) throws HibernateException {
        ((EventSource) delegate).persist(paramString, paramObject, paramMap);
    }

    public void persistOnFlush(String paramString, Object paramObject, Map paramMap) {
        ((EventSource) delegate).persistOnFlush(paramString, paramObject, paramMap);
    }

    public void refresh(Object paramObject, Map paramMap) throws HibernateException {
        ((EventSource) delegate).refresh(paramObject, paramMap);
    }

    /* public void saveOrUpdateCopy(String paramString, Object paramObject, Map paramMap) throws HibernateException
    {
       ((EventSource) delegate).saveOrUpdateCopy(paramString, paramObject, paramMap);
    }*/

    public void delete(String paramString, Object paramObject, boolean paramBoolean, Set paramSet) {
        ((EventSource) delegate).delete(paramString, paramObject, paramBoolean, paramSet);
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        return delegate.buildLockRequest(lockOptions);
    }

    /**
     * Apply non-flushed changes from a different session to this session. It is assumed
     * that this SessionImpl is "clean" (e.g., has no non-flushed changes, no cached entities,
     * no cached collections, no queued actions). The specified NonFlushedChanges object cannot
     * be bound to any session.
     * <p/>
     *
     * @param nonFlushedChanges the non-flushed changes
     */
    @Override
    public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
        delegate.applyNonFlushedChanges(nonFlushedChanges);
    }

    /**
     * Provides access to JDBC connections
     *
     * @return The contract for accessing JDBC connections.
     */
    @Override
    public JdbcConnectionAccess getJdbcConnectionAccess() {
        return delegate.getJdbcConnectionAccess();
    }

    /**
     * Hide the changing requirements of entity key creation
     *
     * @param id        The entity id
     * @param persister The entity persister
     * @return The entity key
     */
    @Override
    public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
        return delegate.generateEntityKey(id, persister);
    }

    /**
     * Hide the changing requirements of cache key creation.
     *
     * @param id               The entity identifier or collection key.
     * @param type             The type
     * @param entityOrRoleName The entity name or collection role.
     * @return The cache key
     */
    @Override
    public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
        return delegate.generateCacheKey(id, type, entityOrRoleName);
    }

    /**
     * Disable automatic transaction joining.  The really only has any effect for CMT transactions.  The default
     * Hibernate behavior is to auto join any active JTA transaction (register {@link javax.transaction.Synchronization}).
     * JPA however defines an explicit join transaction operation.
     * <p/>
     * See javax.persistence.EntityManager#joinTransaction
     */
    @Override
    public void disableTransactionAutoJoin() {
        delegate.disableTransactionAutoJoin();
    }

    /**
     * Return changes to this session that have not been flushed yet.
     *
     * @return The non-flushed changes.
     */
    @Override
    public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
        return delegate.getNonFlushedChanges();
    }

    /**
     * Retrieve access to the session's transaction coordinator.
     *
     * @return The transaction coordinator.
     */
    @Override
    public TransactionCoordinator getTransactionCoordinator() {
        return delegate.getTransactionCoordinator();
    }

    /**
     * Get the load query influencers associated with this session.
     *
     * @return the load query influencers associated with this session;
     *         should never be null.
     */
    @Override
    public LoadQueryInfluencers getLoadQueryInfluencers() {
        return delegate.getLoadQueryInfluencers();
    }

    /**
     * Execute the given callback, making sure it has access to a viable JDBC {@link java.sql.Connection}.
     *
     * @param callback The callback to execute .
     * @return The LOB created by the callback.
     */
    @Override
    public <T> T execute(Callback<T> callback) {
        return delegate.execute(callback);
    }
}
