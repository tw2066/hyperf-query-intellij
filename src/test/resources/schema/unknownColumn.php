<?php Schema::table('users', function (\Hyperf\Database\Schema\Blueprint $table) {
    $table->renameColumn('old_email', 'email');
});
