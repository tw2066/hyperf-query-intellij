<?php Schema::table('users', function (\Hyperf\Database\Schema\Blueprint $table) {
    $table->unique(['id', 'email', '<caret>']);
});
