SolarNetwork.DRAS.User = function(config) {
	config = typeof config === 'object' ? config : {};

	var me = this;
	var contextUser = undefined;
	var programContext = undefined;
	
	this.init = function(cfg) {
		contextUser = typeof cfg.contextUser === 'string' ? cfg.contextUser : undefined;
		programContext = typeof cfg.programContext === 'string' ? cfg.programContext : undefined;
	};
		
	this.userUrl = function(path) {
		return contextUser + path;
	};
		
	this.programUrl = function(path) {
		return programContext + path;
	};
	
	this.populateUser = function(userDivId, userInfo) {
		SolarNetwork.debug('populating user %s', userInfo);
		$('#editUserForm')[0].reset();
		$(userDivId + ' input[name="roles"]').removeAttr('checked');
		
		$(userDivId +' input[name="user.id"]').val(userInfo.user.id);
		$(userDivId +' input[name="user.username"]').val(userInfo.user.username);
		$(userDivId +' input[name="user.displayName"]').val(userInfo.user.displayName);
		$(userDivId +' .userCreatedDate').html(userInfo.user.created);
		for (var i = 0; i < userInfo.roles.length; i++ ) {
			$(userDivId + ' input[name="roles"][value="' + userInfo.roles[i].id + '"]').attr('checked', true);
		}
		if (userInfo.programs) {
			for (var i = 0; i < userInfo.programs.length; i++ ) {
				$(userDivId + ' input[name="programs"][value="' + userInfo.programs[i] + '"]').attr('checked', true);
			}
		}
		if (userInfo.user.contactInfo) {
			for (var i = 0; i < userInfo.user.contactInfo.length; i++ ) {
				var priority = userInfo.user.contactInfo[i].priority;
				SolarNetwork.debug('Populating user contact kind: %s contact: %s priority: %s', userInfo.user.contactInfo[i].kind, userInfo.user.contactInfo[i].contact, priority);
				$(userDivId + ' input[name="user.contactInfo[' + (priority - 1) + '].priority"]').val(priority);
				$(userDivId + ' select[name="user.contactInfo[' + (priority - 1) + '].kind"]').val(userInfo.user.contactInfo[i].kind);
				$(userDivId + ' input[name="user.contactInfo[' + (priority - 1) + '].contact"]').val(userInfo.user.contactInfo[i].contact);
			}
		}
	};
	
	this.loadUserPrograms = function(userProgramDivId, callback) {
		$(userProgramDivId).html('');// Clear previous entries
		$.getJSON(this.programUrl('/findPrograms.json'), function(data) {
			for (var i = 0; i < data.result.length; i++ ) {
				$(userProgramDivId).append('<input type="checkbox" name="programs" class="userProgram" value="' + data.result[i].id + '">' + data.result[i].name + '<br>');
			}
			// After the programs have been loaded we call the callback
			if (callback) {
				callback();
			}
		});
	};
	
	/**
	 * @param userTableId The JQ id of the table to add the user to. e.g. '#usermTable'
	 * @param users The list of user objects to add to the table
	 */
	this.loadUserTable = function(userTableId, users) {
		var table = $(userTableId).dataTable({
			"bRetrieve" : true,
			"bJQueryUI": true,
			"aoColumns": [{ sWidth : "20%" }, 
			              { sWidth : "30%" },
			              { sWidth : "50%" }],
		});
		table.fnClearTable();
		
		SolarNetwork.DRAS.addItemsToTable(userTableId, users, 'user');
	};
	
	this.setupUsersPage = function(userTableId) {

		$('#newUserForm')[0].reset();
		me.loadUserPrograms('#newUserForm .userPrograms');
		
		$('#newUserButton').click(function() {
			$('#newUserForm')[0].reset();
			// Load the programs then show the panel
			me.loadUserPrograms('#newUserForm .userPrograms', function() {
				SolarNetwork.DRAS.showCreatePanel('#newUserPanel', '#editUserPanel');
			});
		});
		
		$.getJSON(this.userUrl('/findUsers.json'), function(data) {
				me.loadUserTable(userTableId, data.result);
		});
		
		// setup newUserForm
		$("#newUserForm").validate();
		$('#newUserForm').ajaxForm({
			url : this.userUrl('/admin/addUser.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.debug('Posting data to server: %s', array);
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Created new user (%s): %s', status, data);
				SolarNetwork.DRAS.addItemToTable(userTableId, data.result, 'user');
				//SolarNetwork.DRAS.populateUser('#editUserPanel', data);
				me.loadUser('#editUserPanel', data.result.id);
				SolarNetwork.DRAS.showEditPanel('#newUserPanel', '#editUserPanel');
				$('#newUserForm')[0].reset();
			}
		});
		
		// setup editUserForm
		$("#editUserForm").validate();
		$('#editUserForm').ajaxForm({
			url : this.userUrl('/admin/saveUser.json'),
			dataType : 'json',
			traditional : true,
			beforeSubmit : function(array) {
				SolarNetwork.debug('Posting data to server: %s', array);
				SolarNetwork.emptyStringRemove(array);
			},
			success : function(data, status) {
				SolarNetwork.debug('Edited user (%s): %s', status, data.result.id);

				// TODO Would be a lot nicer to only refresh relevant row rather than entire table
				$.getJSON(me.userUrl('/findUsers.json'), function(userListData) {
					me.loadUserTable(userTableId, userListData.result);
				});
				
				//SolarNetwork.DRAS.populateUser('#editUserPanel', data);
				me.loadUser('#editUserPanel', data.result.id);
				
				SolarNetwork.DRAS.showEditPanel('#newUserPanel', '#editUserPanel');
			}
		});
	};

	/**
	 * Loads a user into the specified form.
	 * 
	 * @param userDivId The JQ id of the div to load the details into.
	 * @param userId The id of the user to load.
	 */
	this.loadUser = function(userDivId, userId) {
		SolarNetwork.debug('Loading details for user: %d', userId);

		// Loads the programs available to the user
		this.loadUserPrograms(userDivId + ' .userPrograms', function() {
			// Once loaded we can load the user details
			$.getJSON(me.userUrl('/user.json?userId='+userId), function(data) {
				me.populateUser(userDivId, data.result);
			});
		});
	};
	
	this.init(config);
};

var userHelper = new SolarNetwork.DRAS.User({contextUser : '/solardras/u/user', programContext : '/solardras/u/pro'});

//Override to display edit user panel
SolarNetwork.DRAS.showUserPanel = function(userId) {
	SolarNetwork.DRAS.showEditPanel('#newUserPanel', '#editUserPanel');
	userHelper.loadUser('#editUserPanel', userId);
};
