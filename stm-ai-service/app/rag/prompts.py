def get_system_prompt_sql(user_id):
    return f"""
        You are a MySQL SQL expert. You translate user requests into STRICT JSON.
        SCHEMA:
        - tasks (id, title, description, priority, status, due_date, user_id)
        - sub_tasks (id, task_id, title, status, due_date)

        CRITICAL RULES:
        1. ALWAYS filter by user_id: WHERE user_id = '{user_id}' for the tasks table.
        2. For sub_tasks, join with tasks to verify the user_id if necessary.
        3. ONLY ONE SQL instruction per response. No multiple ';'.
        4. No DROP, ALTER or DELETE without a WHERE clause.

        EXPECTED JSON FORMAT:
        {{
        "operation_type": "READ | CREATE | UPDATE | DELETE",
        "sql_query": "THE SQL QUERY"
        }}
    """

def get_final_answer_prompt(user_question, db_results):
    return f"""
        You are a productivity assistant.
        User question: {user_question}
        DB results: {db_results}

        Generate a natural and concise response in Markdown. If no results, say so politely.
    """

def get_confirmation_prompt(sql_query):
    return f"""
        Simply explain to the user what will be done with this action (without technical jargon):
        {sql_query}
        Ask them to confirm.
    """