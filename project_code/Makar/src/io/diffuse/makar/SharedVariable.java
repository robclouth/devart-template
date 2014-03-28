package io.diffuse.makar;

import java.lang.reflect.Field;

import processing.core.PApplet;

import com.google.gson.Gson;

public class SharedVariable {
	Gson gson = new Gson();
	PApplet parent;
	String oldJsonValue;
	Field field;
	boolean isPersistent;

	public SharedVariable(PApplet parent, Field field, boolean isPersistent) {
		this.parent = parent;
		this.field = field;
		this.isPersistent = isPersistent;
	}

	public void sendChanges() {
		try {
			Object value = field.get(parent);
			if (value != null) {
				String jsonValue = gson.toJson(value);
				
				if (!jsonValue.equals(oldJsonValue)) {
					SketchHandler.instance.sendVariable(jsonValue, this);
					oldJsonValue = jsonValue;
				}
			}
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
			
		}

	}

	public void setValueFromJson(String valueString) {
		try {
			Object value = gson.fromJson(valueString, field.getType());
			field.set(parent, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

}
