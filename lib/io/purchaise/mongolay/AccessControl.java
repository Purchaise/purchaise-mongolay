package io.purchaise.mongolay;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by agonlohaj on 29 Oct, 2020
 */
public class AccessControl {
	// access level per row
	public static final String READ_ACL = "readACL";
	public static final String WRITE_ACL = "writeACL";

	private AccessLevelType type;
	private List<String> userRoles;

	/**
	 * constructor
	 */
	public AccessControl(List<String> userRoles, AccessLevelType type) {
		this.type = type;
		this.userRoles = userRoles;
	}

	/**
	 * applies the access filter to the query
	 *
	 * @return
	 */
	public List<Bson> applyAccessFilter() {
		List<Bson> query = new ArrayList<>();
		boolean isRead = type == AccessLevelType.READ;
		boolean isWrite = type == AccessLevelType.WRITE;
		if (isRead) {
			// check if both don't exists, that means its a public thing
			Bson notSet = Filters.and(Filters.exists(READ_ACL, false), Filters.exists(WRITE_ACL, false));
			// check if both their sizes are empty
			Bson bothEmpty = Filters.and(Filters.size(READ_ACL, 0), Filters.size(WRITE_ACL, 0));
			// if one of the above hits, its all public, we assume

			// otherwise check if role is contained on one of them
			Bson roleCheck = Filters.or(Filters.in(READ_ACL, userRoles), Filters.in(WRITE_ACL, userRoles));
			query.add(Filters.or(
					notSet,
					bothEmpty,
					roleCheck
			));
		}
		if (isWrite) {
			// check if both don't exists, that means its a public thing
			Bson notSet = Filters.and(Filters.exists(READ_ACL, false), Filters.exists(WRITE_ACL, false));
			// check if both their sizes are empty
			Bson bothEmpty = Filters.and(Filters.size(READ_ACL, 0), Filters.size(WRITE_ACL, 0));

			// otherwise check if role is within Write access
			Bson roleCheck = Filters.in(WRITE_ACL, userRoles);
			query.add(Filters.or(
					notSet,
					bothEmpty,
					roleCheck
			));
		}
		return query;
	}

	public Document isAccessible(RelayCollection<Document> collection, Bson filter, ObjectId id) throws RelayException {
		if (id == null) {
			return null;
		}
		List<Bson> findFilters = new ArrayList<>();
		findFilters.add(Filters.eq("_id", id));
		if (filter != null) {
			findFilters.add(filter);
		}
		FindIterable<Document> find = collection.find().withoutAccess().filter(Filters.and(findFilters));
		Document document = find.first();
		if (document == null) {
			return null;
		}
		List<String> readACL = document.get(READ_ACL, new ArrayList<>());
		List<String> writeACL = document.get(WRITE_ACL, new ArrayList<>());
		if (!RelayModel.isAccessible(type, userRoles, readACL, writeACL)) {
			throw new RelayException(Http.Status.FORBIDDEN, "access_forbidden");
		}
		return document;
	}

	public <T extends RelayModel> T isAccessibleOnCollection(RelayCollection<T> collection, Bson filter, ObjectId id) throws RelayException {
		if (id == null) {
			return null;
		}
		List<Bson> findFilters = new ArrayList<>();
		findFilters.add(Filters.eq("_id", id));
		if (filter != null) {
			findFilters.add(filter);
		}
		FindIterable<T> find = collection.find().withoutAccess().filter(Filters.and(findFilters));
		T document = find.first();
		if (document == null) {
			return null;
		}
		if (!document.isAccessible(type, userRoles)) {
			throw new RelayException(Http.Status.FORBIDDEN, "access_forbidden");
		}
		return document;
	}
}