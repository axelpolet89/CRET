package com.crawljax.plugins.cilla.data;

public class MProperty
{

	private String _name;
	private String _value;
	private String _status;
	private boolean _isEffective;
	private boolean _isImportant;

	public MProperty(String name, String value, boolean isImportant)
	{
		_name = name;
		_value = value;
		_status = "notset";
		_isImportant = isImportant;
	}

	public String GetName()
	{
		return _name;
	}

	public String GetValue()
	{
		return _value;
	}

	public boolean IsImportant() { return _isImportant; }

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

	public String Print()
	{
		String result = _name + ": " + _value;

		if(_isImportant)
			return result + " !important;";

		return result + ";";
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		return "{ " + _name + " : " + _value + " " + (_isEffective ? "Effective" : "Ineffective")
		        + " }";
	}
}
