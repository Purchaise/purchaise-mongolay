package io.purchaise.mongolay;

import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class RelayModel implements Cloneable, Serializable {
	protected abstract ObjectId getId();
	protected abstract void setId(ObjectId id);
	protected abstract void setUpdatedAt(Long updatedAt);
	protected abstract Set<String> getWriteACL();
	protected abstract Set<String> getReadACL();

	public boolean isAccessible (AccessLevelType type, List<String> roles) {
		return RelayModel.isAccessible(type, roles, getReadACL(), getWriteACL());
	}

	public static boolean isAccessible (AccessLevelType type, List<String> roles, Collection<String> readACL, Collection<String> writeACL) {
		switch (type) {
			case NONE:
				return true;
			case WRITE:
				return RelayModel.isWritableFromRoles(roles, readACL, writeACL);
			case READ:
				return RelayModel.isReadableFromRoles(roles, readACL, writeACL);
		}
		return true;
	}

	public boolean isReadableFromRoles (List<String> roles) {
		return isReadableFromRoles(roles, getReadACL(), getWriteACL());
	}

	public static boolean isReadableFromRoles (List<String> roles, Collection<String> readACL, Collection<String> writeACL) {
		if (RelayModel.isWritableFromRoles(roles, readACL, writeACL)) {
			return true;
		}
		if (readACL.contains("*")) {
			return true;
		}
		for (String role: roles) {
			if (readACL.contains(role)) {
				return true;
			}
		}
		return false;
	}

	public boolean isWritableFromRoles (List<String> roles) {
		return RelayModel.isWritableFromRoles(roles, getReadACL(), getWriteACL());
	}

	public static boolean isWritableFromRoles (List<String> roles, Collection<String> readACL, Collection<String> writeACL) {
		if (writeACL.size() == 0 && readACL.size() == 0) {
			return true;
		}
		if (writeACL.contains("*")) {
			return true;
		}
		for (String role: roles) {
			if (writeACL.contains(role)) {
				return true;
			}
		}
		return false;
	}
}
