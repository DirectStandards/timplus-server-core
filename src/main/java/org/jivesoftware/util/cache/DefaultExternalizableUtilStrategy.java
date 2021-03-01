package org.jivesoftware.util.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;

public class DefaultExternalizableUtilStrategy implements ExternalizableUtilStrategy
{

	@Override
	public void writeStringMap(DataOutput out, Map<String, String> stringMap) throws IOException 
	{
		out.writeInt(stringMap.size());
		for (Entry<String, String> entry : stringMap.entrySet())
		{
			this.writeSafeUTF(out, entry.getKey());
			this.writeSafeUTF(out, entry.getValue());
		}
		
	}

	@Override
	public Map<String, String> readStringMap(DataInput in) throws IOException 
	{
		final Map<String, String> theMap = new HashMap<>();
		
		int retVal = in.readInt();
		for (int i = 0; i < retVal; ++i)
		{
			final String key = this.readSafeUTF(in);
			final String value = this.readSafeUTF(in);
			
			theMap.put(key, value);
		}
		
		return theMap;
	}

	@Override
	public void writeLongIntMap(DataOutput out, Map<Long, Integer> map) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<Long, Integer> readLongIntMap(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeStringList(DataOutput out, List<String> stringList) throws IOException 
	{
		out.writeInt(stringList.size());
		for (String str : stringList)
			this.writeSafeUTF(out, str);
		
	}

	@Override
	public List<String> readStringList(DataInput in) throws IOException 
	{
		final List<String> list = new LinkedList<>();
		
		int retVal = in.readInt();
		
		for (int i = 0; i < retVal; ++i)
			list.add(this.readSafeUTF(in));
		
		return list;
	}

	@Override
	public void writeLongArray(DataOutput out, long[] array) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long[] readLongArray(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeLong(DataOutput out, long value) throws IOException 
	{
		out.writeLong(value);
		
	}

	@Override
	public long readLong(DataInput in) throws IOException 
	{
		return in.readLong();
	}

	@Override
	public void writeBoolean(DataOutput out, boolean value) throws IOException
	{
		out.writeBoolean(value);
		
	}

	@Override
	public boolean readBoolean(DataInput in) throws IOException 
	{
		return in.readBoolean();
	}

	@Override
	public void writeByteArray(DataOutput out, byte[] value) throws IOException {
		
		out.writeInt(value.length);
		out.write(value);
		
	}

	@Override
	public byte[] readByteArray(DataInput in) throws IOException {
		final int length = in.readInt();
		final byte[] bytes = new byte[length];
		
		in.readFully(bytes);
		
		return bytes;
	}

	@Override
	public void writeSerializable(DataOutput out, Serializable value) throws IOException 
	{
		
		final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		final ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
		
		try
		{
			objOutStream.writeObject(objOutStream);
		
			final byte[] bytes = outStream.toByteArray();
	
			out.writeInt(bytes.length);
			out.write(bytes);
		}
		catch (Exception e)
		{
			/* no - op for now */
			//throw new IllegalStateException("Cannot serialize object.", e);
		}
	}

	@Override
	public Serializable readSerializable(DataInput in) throws IOException 
	{
		
		final int length = in.readInt();
		final byte[] bytes = new byte[length];
		
		in.readFully(bytes);
		
		final ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
		final ObjectInputStream obInStream = new ObjectInputStream(inStream);
		
		try
		{
			return (Serializable)obInStream.readObject();
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Failed to deserialized object.", e);
		}
	}

	@Override
	public void writeSafeUTF(DataOutput out, String value) throws IOException 
	{
		final byte[] bytes = StringUtils.getBytesUtf8(value);
		
		if (bytes == null)
			out.writeInt(0);
		else
		{
			out.writeInt(bytes.length);
			out.write(bytes);
		}
	}

	@Override
	public String readSafeUTF(DataInput in) throws IOException {
		final int length = in.readInt();
		
		if (length == 0)
			return "";
		
		final byte[] bytes = new byte[length];
		
		in.readFully(bytes);
		
		return StringUtils.newStringUtf8(bytes);

	}

	@Override
	public void writeExternalizableCollection(DataOutput out, Collection<? extends Externalizable> value)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int readExternalizableCollection(DataInput in, Collection<? extends Externalizable> value,
			ClassLoader loader) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeSerializableCollection(DataOutput out, Collection<? extends Serializable> value)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int readSerializableCollection(DataInput in, Collection<? extends Serializable> value, ClassLoader loader)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeExternalizableMap(DataOutput out, Map<String, ? extends Externalizable> map) throws IOException 
	{
		out.writeInt(map.size());
		for (Entry<String, ? extends Externalizable> entry : map.entrySet())
		{
			this.writeSafeUTF(out, entry.getKey());
			this.writeSafeUTF(out, entry.getValue().getClass().getName());
			entry.getValue().writeExternal((ObjectOutput)out);
		}
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int readExternalizableMap(DataInput in, Map<String, ? extends Externalizable> map, ClassLoader loader)
			throws IOException 
	{
		final Map theMap = map;
		
		int retVal = in.readInt();
		for (int i = 0; i < retVal; ++i)
		{
			try
			{
				final String key = this.readSafeUTF(in);
				final String valueClassName = this.readSafeUTF(in);
				Class<Externalizable> valueClass = (Class<Externalizable>)this.getClass().getClassLoader().loadClass(valueClassName);
				final Externalizable value = valueClass.newInstance();
	
				value.readExternal((ObjectInput)in);
				
				theMap.put(key, value);
			}
			catch (Exception e)
			{
				throw new IllegalStateException("Failed to read externalizable object.");
			}
		}
		
		return retVal;
	}

	@Override
	public void writeSerializableMap(DataOutput out, Map<? extends Serializable, ? extends Serializable> map)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int readSerializableMap(DataInput in, Map<? extends Serializable, ? extends Serializable> map,
			ClassLoader loader) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeStringsMap(DataOutput out, Map<String, Set<String>> map) throws IOException 
	{
		out.writeInt(map.size());
		for (Entry<String, Set<String>> entry : map.entrySet())
		{
			this.writeSafeUTF(out, entry.getKey());
			out.writeInt(entry.getValue().size());
			for (String str : entry.getValue())
			{
				this.writeSafeUTF(out, str);
			}
		}
		
	}

	@Override
	public int readStringsMap(DataInput in, Map<String, Set<String>> map) throws IOException 
	{
		int retVal = in.readInt();
		for (int i = 0; i < retVal; ++i)
		{
			final String key = this.readSafeUTF(in);
			final int setLen = this.readInt(in);
			final Set<String> entry = new HashSet<>();
			
			for (int j = 0; j < setLen; ++j)
			{
				entry.add(this.readSafeUTF(in));
			}
			
			map.put(key,  entry);
		}
		
		return retVal;
	}

	@Override
	public void writeStrings(DataOutput out, Collection<String> collection) throws IOException 
	{
		out.writeInt(collection.size());
		for (String str : collection)
			this.writeSafeUTF(out, str);
		
	}

	@Override
	public int readStrings(DataInput in, Collection<String> collection) throws IOException 
	{
		int retVal = in.readInt();
		
		for (int i = 0; i < retVal; ++i)
			collection.add(this.readSafeUTF(in));
		
		return retVal;
	}

	@Override
	public void writeInt(DataOutput out, int value) throws IOException 
	{
		out.writeInt(value);
		
	}

	@Override
	public int readInt(DataInput in) throws IOException 
	{
		return in.readInt();
	}

}
