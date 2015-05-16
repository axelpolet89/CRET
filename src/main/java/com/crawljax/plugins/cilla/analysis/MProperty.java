package com.crawljax.plugins.cilla.analysis;

public class MProperty
{

	private String _name;
	private String _value;
	private String _status = "notset";
	private boolean _isEffective;

	public MProperty(String name, String value)
	{
		_name = name;
		_value = value;
	}

	public String GetName()
	{
		return _name;
	}

	public String GetValue()
	{
		return _value;
	}

	public boolean IsEffective()
	{
		return _isEffective;
	}

	public void SetEffective(boolean effective)
	{
		_isEffective = effective;
	}

	public String GetStatus()
	{
		return _status;
	}

	public void SetStatus(String status)
	{
		_status = status;
	}

	public int GetSize()
	{
		return (_name.getBytes().length+ _value.getBytes().length);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		return "{ " + _name + " : " + _value + " " + (_isEffective ? "Effective" : "Ineffective")
		        + " }";
	}
}
