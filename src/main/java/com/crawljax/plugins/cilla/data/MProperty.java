package com.crawljax.plugins.cilla.data;

/**
 * Represents a style declaration or CSS property that is contained inside a CSS rule
 * A property has a name, value and may be important
 * After analysis, a property may be overridden, thereby rendered ineffective
 */
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

	/** Getter */
	public String GetName()
	{
		return _name;
	}

	/** Getter */
	public String GetValue()
	{
		return _value;
	}

	/** Getter */
	public boolean IsImportant()
	{
		return _isImportant;
	}

	/** Getter */
	public boolean IsEffective()
	{
		return _isEffective;
	}

	/** Getter */
	public String GetStatus()
	{
		return _status;
	}


	public void SetEffective(boolean effective)
	{
		_isEffective = effective;
	}

	public void SetStatus(String status)
	{
		_status = status;
	}

	public int ComputeSizeBytes()
	{
		return (_name.getBytes().length+ _value.getBytes().length);
	}

	/**
	 * Transform this property into valid CSS syntax
	 * @return
	 */
	public String Print()
	{
		String result = _name + ": " + _value;

		if(_isImportant)
			return result + " !important;";

		return result + ";";
	}

	@Override
	public String toString()
	{
		return "{ " + _name + " : " + _value + " " + (_isImportant ? "!important ": "") + (_isEffective ? "Effective" : "Ineffective") + " }";
	}
}
