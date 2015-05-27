package com.crawljax.plugins.csssuite.data;

/**
 * Represents a style declaration or CSS property that is contained inside a CSS rule
 * A property has a name, value and may be important
 * After analysis, a property may be overridden, thereby rendered ineffective
 */
public class MProperty
{
	private String _name;
	private String _originalValue;
	private String _normalizedValue;
	private String _status;
	private boolean _isEffective;
	private boolean _isImportant;

	public MProperty(String name, String value, boolean isImportant)
	{
		_name = name;
		_originalValue = value.trim();
		_normalizedValue = value.trim();
		_status = "notset";
		_isImportant = isImportant;
	}

	/** Getter */
	public String GetName()
	{
		return _name;
	}

	/** Getter */
	public String GetOriginalValue()
	{
		return _originalValue;
	}

	/** Getter */
	public String GetValue()
	{
		return _normalizedValue;
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


	/**
	 *
	 * @param value
	 */
	public void SetNormalizedValue(String value)
	{
		_normalizedValue = value;
	}


	/**
	 *
	 * @param effective
	 */
	public void SetEffective(boolean effective)
	{
		_isEffective = effective;
	}


	/**
	 *
	 * @param status
	 */
	public void SetStatus(String status)
	{
		_status = status;
	}


	/**
	 *
	 * @return
	 */
	public int ComputeSizeBytes()
	{
		return (_name.getBytes().length+ _normalizedValue.getBytes().length);
	}


	/**
	 * Transform this property into valid CSS syntax
	 * @return valid CSS syntax
	 */
	public String Print()
	{
		String result = _name + ": " + _normalizedValue;

		if(_isImportant)
			return result + " !important;";

		return result + ";";
	}


	/**
	 * @return a short string that may be used as a key in HashMap comparisons
	 */
	public String AsKey()
	{
		String result = _name + "-" + _normalizedValue;
		if(_isImportant)
			return result + "-" + "!";

		return result;
	}

	@Override
	public String toString()
	{
		return "{ " + _name + " : " + _normalizedValue + " " + (_isImportant ? "!important ": "") + (_isEffective ? "Effective" : "Ineffective") + " }";
	}
}