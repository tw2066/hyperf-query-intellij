<?php Schema::table('users', function (\Hyperf\Database\Schema\Blueprint $table) {
    $table->dropUnique('users_email_uindex');
});
